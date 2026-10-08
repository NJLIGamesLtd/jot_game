import 'dart:async';
import 'dart:isolate';

import 'transport.dart';

class IsolateTransport implements CoreTransport {
  final ReceivePort _events = ReceivePort();
  final Completer<void> _ready = Completer<void>();
  final Map<int, Completer<String?>> _pending = {};
  late final StreamSubscription<dynamic> _subscription;
  Isolate? _isolate;
  SendPort? _port;
  Object? _failure;
  int _nextId = 1;
  bool _closed = false;
  Future<void>? _closeFuture;

  IsolateTransport() {
    // An isolate can report a startup failure before spawn's Future resumes.
    // A later await still receives the error, without an unhandled gap.
    _ready.future.ignore();
    _subscription = _events.listen(_onMessage);
  }

  /// Start the owning worker. The callback also supports deterministic tests of
  /// worker startup, crashes, and protocol failures without a native crash.
  Future<void> start(Future<Isolate?> Function(SendPort) spawn) async {
    try {
      _isolate = await spawn(_events.sendPort);
      await _ready.future;
    } catch (_) {
      _dispose();
      rethrow;
    }
  }

  void _onMessage(dynamic message) {
    if (message is SendPort) {
      _port = message;
      if (!_ready.isCompleted) _ready.complete();
    } else if (message is (int, String?, String?)) {
      final (id, response, error) = message;
      final pending = _pending.remove(id);
      if (pending == null) return;
      if (error != null) {
        pending.completeError(StateError(error));
      } else {
        pending.complete(response);
      }
    } else {
      // Dart routes uncaught isolate errors and exits to this same port.
      // Keep it alive after startup so commands cannot hang on a dead worker.
      final detail = message is List && message.isNotEmpty
          ? message.first.toString()
          : message?.toString() ?? 'The native engine worker exited.';
      _fail(StateError(detail));
    }
  }

  void _fail(Object error) {
    _failure ??= error;
    if (!_ready.isCompleted) _ready.completeError(error);
    final pending = _pending.values.toList();
    _pending.clear();
    for (final request in pending) {
      request.completeError(error);
    }
  }

  Future<String?> _request(String? command) {
    if (_failure != null) return Future.error(_failure!);
    final port = _port;
    if (port == null) return Future.error(StateError('Engine is not ready.'));
    final id = _nextId++;
    final reply = Completer<String?>();
    _pending[id] = reply;
    port.send((id, command));
    return reply.future;
  }

  @override
  Future<String> execute(String command) async {
    if (_closed) throw StateError('Engine is closed.');
    if (command.contains('\u0000')) {
      throw ArgumentError.value(command, 'command', 'Contains a null byte.');
    }
    final response = await _request(command);
    if (response == null) {
      throw StateError('Native engine returned no response.');
    }
    return response;
  }

  @override
  Future<void> close() {
    final closing = _closeFuture;
    if (closing != null) return closing;
    _closed = true;
    final future = _request(null).then<void>((_) {}).whenComplete(_dispose);
    _closeFuture = future;
    return future;
  }

  void _dispose() {
    _events.close();
    unawaited(_subscription.cancel());
    _isolate?.kill(priority: Isolate.immediate);
  }
}
