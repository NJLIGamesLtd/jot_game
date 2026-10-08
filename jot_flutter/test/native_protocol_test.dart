import 'dart:async';
import 'dart:ffi';
import 'dart:isolate';

import 'package:ffi/ffi.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:jot_core/src/bridge_stub.dart' as stub;
import 'package:jot_core/src/creation.dart';
import 'package:jot_core/src/isolate_transport.dart';
import 'package:jot_core/src/native_worker.dart';

class _FakeApi implements EngineApi {
  final Map<int, String> allocated = {};
  final List<int> freed = [];
  int nextPointer = 100;
  int creates = 0;
  int destroys = 0;
  int attaches = 0;
  int detaches = 0;
  bool returnNull = false;
  bool noHandle = false;
  String? allocationFailure;
  Object? creationFailure;
  Object? executionFailure;

  @override
  Pointer<Utf8> allocate(String text) {
    if (text == allocationFailure) throw StateError('allocation failed');
    allocated[nextPointer] = text;
    return Pointer<Utf8>.fromAddress(nextPointer++);
  }

  @override
  void free(Pointer<Utf8> text) {
    expect(allocated.remove(text.address), isNotNull);
    freed.add(text.address);
  }

  @override
  Pointer<Void> create(Pointer<Utf8> dictionary, int seed, int robot) {
    creates++;
    if (creationFailure != null) throw creationFailure!;
    expect(allocated[dictionary.address], 'dictionary');
    expect(seed, 123);
    expect(robot, -1);
    return noHandle ? nullptr : Pointer<Void>.fromAddress(42);
  }

  @override
  String? execute(Pointer<Void> handle, Pointer<Utf8> command) {
    expect(handle.address, 42);
    if (executionFailure != null) throw executionFailure!;
    return returnNull ? null : 'response:${allocated[command.address]}';
  }

  @override
  void destroy(Pointer<Void> handle) {
    expect(handle.address, 42);
    destroys++;
  }

  @override
  void attach(Finalizable owner, Pointer<Void> handle) => attaches++;

  @override
  void detach(Finalizable owner) => detaches++;
}

void main() {
  test('creation accepts every boundary and rejects invalid ABI input', () {
    for (final seed in [0, 0xffffffff]) {
      for (final robot in [-1, 0, 1]) {
        validateCoreCreation('AT\n', seed, robot);
      }
    }
    for (final seed in [-1, 0x100000000]) {
      expect(() => validateCoreCreation('AT', seed, 1), throwsArgumentError);
    }
    for (final robot in [-2, 2]) {
      expect(() => validateCoreCreation('AT', 1, robot), throwsArgumentError);
    }
    expect(() => validateCoreCreation('AT\u0000', 1, 1), throwsArgumentError);
    expect(() => stub.createCore('AT'), throwsUnsupportedError);
  });

  test(
    'native worker frees startup allocations on both startup failure paths',
    () async {
      for (final failure in ['allocate', 'create', 'null']) {
        final api = _FakeApi();
        if (failure == 'allocate') api.allocationFailure = 'dictionary';
        if (failure == 'create') {
          api.creationFailure = StateError('create failed');
        }
        if (failure == 'null') api.noHandle = true;
        final port = ReceivePort();
        final messages = StreamIterator<dynamic>(port);
        runNativeEngine((port.sendPort, 'dictionary', 123, -1), api);
        expect(await messages.moveNext(), isTrue);
        expect(
          messages.current,
          contains(failure == 'null' ? 'could not load' : 'could not start'),
        );
        expect(api.allocated, isEmpty);
        expect(api.destroys, 0);
        expect(api.attaches, 0);
        port.close();
        await messages.cancel();
      }
    },
  );

  test(
    'native worker copies replies, frees failed commands, and detaches ownership on close',
    () async {
      final api = _FakeApi();
      final port = ReceivePort();
      final messages = StreamIterator<dynamic>(port);
      runNativeEngine((port.sendPort, 'dictionary', 123, -1), api);
      expect(await messages.moveNext(), isTrue);
      final requests = messages.current as SendPort;
      expect(api.attaches, 1);

      requests.send((1, 'state'));
      expect(await messages.moveNext(), isTrue);
      expect(messages.current, (1, 'response:state', null));
      expect(api.allocated, isEmpty);

      api.allocationFailure = 'bad allocation';
      requests.send((2, 'bad allocation'));
      expect(await messages.moveNext(), isTrue);
      expect(
        (messages.current as (int, String?, String?)).$3,
        contains('allocation failed'),
      );
      api.executionFailure = StateError('execute failed');
      requests.send((3, 'bad command'));
      expect(await messages.moveNext(), isTrue);
      expect(
        (messages.current as (int, String?, String?)).$3,
        contains('execute failed'),
      );
      expect(api.allocated, isEmpty);

      api.executionFailure = null;
      api.returnNull = true;
      requests.send((4, 'no response'));
      expect(await messages.moveNext(), isTrue);
      expect(
        (messages.current as (int, String?, String?)).$3,
        contains('returned null'),
      );
      api.returnNull = false;
      requests.send((5, 'usable again'));
      expect(await messages.moveNext(), isTrue);
      expect(messages.current, (5, 'response:usable again', null));

      requests.send((6, null));
      expect(await messages.moveNext(), isTrue);
      expect(messages.current, (6, null, null));
      expect(api.detaches, 1);
      expect(api.destroys, 1);
      expect(api.allocated, isEmpty);
      port.close();
      await messages.cancel();
    },
  );

  test(
    'transport rejects commands before startup and cleans up failed spawn',
    () async {
      final notStarted = IsolateTransport();
      await expectLater(notStarted.execute('state'), throwsStateError);
      await expectLater(notStarted.close(), throwsStateError);
      final failedSpawn = IsolateTransport();
      await expectLater(
        failedSpawn.start((_) async => throw StateError('spawn failed')),
        throwsStateError,
      );
    },
  );

  test(
    'transport matches request IDs, handles scoped errors, and closes once',
    () async {
      final transport = IsolateTransport();
      final requests = ReceivePort();
      late SendPort host;
      final subscription = requests.listen((dynamic message) {
        final (id, command) = message as (int, String?);
        host.send((999, 'unknown', null));
        if (command == 'bad') {
          host.send((id, null, 'command rejected'));
        } else if (command == 'empty') {
          host.send((id, null, null));
        } else {
          host.send((id, command == null ? null : 'response:$command', null));
        }
      });
      await transport.start((events) async {
        host = events;
        events.send(requests.sendPort);
        events.send(requests.sendPort); // Duplicate startup event is harmless.
        return null;
      });
      await expectLater(transport.execute('bad'), throwsStateError);
      await expectLater(transport.execute('empty'), throwsStateError);
      await expectLater(transport.execute('state\u0000'), throwsArgumentError);
      expect(
        await Future.wait([
          transport.execute('first'),
          transport.execute('second'),
        ]),
        ['response:first', 'response:second'],
      );
      final close = transport.close();
      expect(identical(close, transport.close()), isTrue);
      await close;
      await expectLater(transport.execute('late'), throwsStateError);
      requests.close();
      await subscription.cancel();
    },
  );

  test(
    'fatal worker events reject startup, pending requests, and future requests',
    () async {
      for (final event in [
        null,
        ['isolate crashed', 'stack trace'],
        [],
        'startup rejected',
      ]) {
        final transport = IsolateTransport();
        await expectLater(
          transport.start((events) async {
            events.send(event);
            return null;
          }),
          throwsStateError,
        );
      }
      final transport = IsolateTransport();
      final requests = ReceivePort();
      final subscription = requests.listen((_) {});
      late SendPort host;
      await transport.start((events) async {
        host = events;
        events.send(requests.sendPort);
        return null;
      });
      final first = expectLater(transport.execute('first'), throwsStateError);
      final second = expectLater(transport.execute('second'), throwsStateError);
      host.send(['worker terminated', 'stack']);
      host.send(null); // Duplicate exit cannot replace the first failure.
      await Future.wait([first, second]);
      await expectLater(transport.execute('third'), throwsStateError);
      await expectLater(transport.close(), throwsStateError);
      requests.close();
      await subscription.cancel();
    },
  );
}
