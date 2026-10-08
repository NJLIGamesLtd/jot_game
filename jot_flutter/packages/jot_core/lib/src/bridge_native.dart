import 'dart:isolate';

import 'creation.dart';
import 'isolate_transport.dart';
import 'native_worker.dart';
import 'transport.dart';

Future<CoreTransport> createCore(
  String dictionary, {
  int seed = 1,
  int robot = 1,
}) async {
  validateCoreCreation(dictionary, seed, robot);
  final transport = IsolateTransport();
  await transport.start(
    (events) => Isolate.spawn(
      _runEngine,
      (events, dictionary, seed, robot),
      onError: events,
      onExit: events,
      errorsAreFatal: true,
    ),
  );
  return transport;
}

void _runEngine((SendPort, String, int, int) input) {
  runNativeEngine(input, const FfiEngineApi());
}
