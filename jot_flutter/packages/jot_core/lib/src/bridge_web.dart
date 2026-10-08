import 'dart:js_interop';

import 'creation.dart';
import 'transport.dart';

@JS('jotBridge')
external _WebBridge get _bridge;

extension type _WebBridge(JSObject _) implements JSObject {
  external JSPromise<JSNumber> create(String dictionary, int seed, int robot);
  external JSPromise<JSString> execute(int handle, String command);
  external JSPromise<JSAny?> close(int handle);
}

Future<CoreTransport> createCore(
  String dictionary, {
  int seed = 1,
  int robot = 1,
}) async {
  validateCoreCreation(dictionary, seed, robot);
  final handle =
      (await _bridge.create(dictionary, seed, robot).toDart).toDartInt;
  if (handle == 0) throw StateError('The WebAssembly engine could not start.');
  return _WebTransport(handle);
}

class _WebTransport implements CoreTransport {
  final int _handle;
  bool _closed = false;
  Future<void>? _closeFuture;

  _WebTransport(this._handle);

  @override
  Future<String> execute(String command) async {
    if (_closed) throw StateError('Engine is closed.');
    if (command.contains('\u0000')) {
      throw ArgumentError.value(command, 'command', 'Contains a null byte.');
    }
    return (await _bridge.execute(_handle, command).toDart).toDart;
  }

  @override
  Future<void> close() {
    final closing = _closeFuture;
    if (closing != null) return closing;
    _closed = true;
    final future = _bridge.close(_handle).toDart.then<void>((_) {});
    _closeFuture = future;
    return future;
  }
}
