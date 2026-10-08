import 'dart:ffi';
import 'dart:isolate';

import 'package:ffi/ffi.dart';

import 'bindings.dart';

/// The ABI boundary is injectable so worker failures can be tested safely.
/// Production always uses [FfiEngineApi]; no rule or game state lives here.
abstract interface class EngineApi {
  Pointer<Utf8> allocate(String text);
  void free(Pointer<Utf8> text);
  Pointer<Void> create(Pointer<Utf8> dictionary, int seed, int robot);
  String? execute(Pointer<Void> handle, Pointer<Utf8> command);
  void destroy(Pointer<Void> handle);
  void attach(Finalizable owner, Pointer<Void> handle);
  void detach(Finalizable owner);
}

class FfiEngineApi implements EngineApi {
  const FfiEngineApi();

  static final NativeFinalizer _finalizer = NativeFinalizer(
    Native.addressOf(jotDestroy),
  );

  @override
  Pointer<Utf8> allocate(String text) => text.toNativeUtf8();

  @override
  void free(Pointer<Utf8> text) => calloc.free(text);

  @override
  Pointer<Void> create(Pointer<Utf8> dictionary, int seed, int robot) =>
      jotCreate(dictionary, seed, robot);

  @override
  String? execute(Pointer<Void> handle, Pointer<Utf8> command) {
    final response = jotExecute(handle, command);
    // The ABI response is borrowed. Copy it before another command can
    // replace the native buffer owned by this handle.
    return response == nullptr ? null : response.toDartString();
  }

  @override
  void destroy(Pointer<Void> handle) => jotDestroy(handle);

  @override
  void attach(Finalizable owner, Pointer<Void> handle) {
    _finalizer.attach(owner, handle, detach: owner);
  }

  @override
  void detach(Finalizable owner) => _finalizer.detach(owner);
}

void runNativeEngine((SendPort, String, int, int) input, EngineApi api) {
  final (host, dictionary, seed, robot) = input;
  Pointer<Utf8> text = nullptr;
  Pointer<Void> handle = nullptr;
  try {
    text = api.allocate(dictionary);
    handle = api.create(text, seed, robot);
  } catch (error) {
    host.send('The native engine could not start: $error');
    return;
  } finally {
    if (text != nullptr) api.free(text);
  }
  if (handle == nullptr) {
    host.send('The C++ engine could not load the dictionary.');
    return;
  }
  final ownedHandle = _OwnedHandle(handle, api);
  final requests = ReceivePort();
  host.send(requests.sendPort);
  requests.listen((dynamic request) {
    final (id, command) = request as (int, String?);
    if (command == null) {
      ownedHandle.close();
      requests.close();
      host.send((id, null, null));
      return;
    }
    Pointer<Utf8> nativeCommand = nullptr;
    try {
      nativeCommand = api.allocate(command);
      final response = api.execute(ownedHandle.pointer, nativeCommand);
      if (response == null) throw StateError('Native engine returned null.');
      host.send((id, response, null));
    } catch (error) {
      host.send((id, null, error.toString()));
    } finally {
      if (nativeCommand != nullptr) api.free(nativeCommand);
    }
  });
}

final class _OwnedHandle implements Finalizable {
  final Pointer<Void> pointer;
  final EngineApi api;

  _OwnedHandle(this.pointer, this.api) {
    api.attach(this, pointer);
  }

  void close() {
    api.detach(this);
    api.destroy(pointer);
  }
}
