import 'dart:js_interop';

import 'package:jot_core/src/bridge_web.dart';
import 'package:jot_core/src/creation.dart';

@JS('testBridgeMode')
external set mode(String value);

@JS('console.log')
external void log(String value);

Future<void> rejects(Future<dynamic> Function() operation, Type type) async {
  try {
    await operation();
  } catch (error) {
    if (error.runtimeType == type ||
        (type == ArgumentError && error is ArgumentError)) {
      return;
    }
    throw StateError('Expected $type, received $error');
  }
  throw StateError('Expected $type, but operation succeeded.');
}

Future<void> main() async {
  validateCoreCreation('AT', 0, -1);
  validateCoreCreation('AT', 0xffffffff, 0);
  for (final seed in [-1, 0x100000000]) {
    await rejects(() => createCore('AT', seed: seed), ArgumentError);
  }
  for (final robot in [-2, 2]) {
    await rejects(() => createCore('AT', robot: robot), ArgumentError);
  }
  await rejects(() => createCore('AT\u0000'), ArgumentError);

  mode = 'zero';
  await rejects(() => createCore('AT'), StateError);
  mode = 'normal';
  final engine = await createCore('AT', seed: 123, robot: -1);
  if (await engine.execute('state') != 'response:state') {
    throw StateError('The bridge must copy the JS string response.');
  }
  await rejects(() => engine.execute('state\u0000'), ArgumentError);
  final closing = engine.close();
  if (!identical(closing, engine.close())) {
    throw StateError('Concurrent close must share the same completion.');
  }
  await closing;
  await rejects(() => engine.execute('late'), StateError);

  mode = 'close failure';
  final failing = await createCore('AT');
  try {
    await failing.close();
    throw StateError('The close failure must propagate.');
  } catch (error) {
    if (!error.toString().contains('close failed')) rethrow;
  }
  await rejects(() => failing.execute('late'), StateError);
  log('Dart WebAssembly bridge validation, JS interop, and lifecycle passed.');
}
