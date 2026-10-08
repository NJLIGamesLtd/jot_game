import 'dart:async';
import 'dart:convert';

import 'package:flutter_test/flutter_test.dart';
import 'package:jot_core/jot_core.dart';
import 'package:jot_flutter/game_controller.dart';
import 'package:shared_preferences/shared_preferences.dart';
import 'package:shared_preferences_platform_interface/shared_preferences_platform_interface.dart';

import 'support/multiplayer_fixture.dart';

class ScriptedCore implements CoreTransport {
  final commands = <Map<String, dynamic>>[];
  final Map<String, dynamic> current = Map<String, dynamic>.from(
    snapshot()['state'] as Map,
  );
  FutureOr<Map<String, dynamic>?> Function(Map<String, dynamic>)? handle;
  bool closed = false;
  bool failSave = false;
  bool failClose = false;
  ScriptedCore() {
    current['robotPlayer'] = 1;
  }
  @override
  Future<String> execute(String command) async {
    final decoded = jsonDecode(command) as Map<String, dynamic>;
    commands.add(decoded);
    if (decoded['op'] == 'save') {
      return jsonEncode({
        'ok': !failSave,
        'save': failSave ? null : {'version': 1, 'marker': 'saved'},
      });
    }
    final result = await handle?.call(decoded);
    return jsonEncode(
      result ?? {'ok': true, 'message': 'Done.', 'state': current},
    );
  }

  @override
  Future<void> close() async {
    closed = true;
    if (failClose) throw StateError('close failure');
  }
}

class FailingPreferencesStore extends InMemorySharedPreferencesStore {
  bool fail = false;
  bool throws = false;
  FailingPreferencesStore() : super.empty();
  @override
  Future<bool> setValue(String type, String key, Object value) async {
    if (fail) {
      if (throws) throw StateError('Disk unavailable');
      return false;
    }
    return super.setValue(type, key, value);
  }
}

GameController local(
  ScriptedCore core, {
  Future<String> Function()? dictionary,
}) => GameController(
  multiplayerApi: FakeMultiplayerApi(),
  initialUri: Uri.parse('https://jot-game.pages.dev/'),
  coreFactory: (_) async => core,
  dictionaryLoader: dictionary ?? () async => 'AT\nCAT',
);

void main() {
  TestWidgetsFlutterBinding.ensureInitialized();
  setUp(() => SharedPreferences.setMockInitialValues({}));

  test(
    'Native replies arriving after disposal do not update the controller snapshot',
    () async {
      final reply = Completer<Map<String, dynamic>>();
      final started = Completer<void>();
      final core = ScriptedCore()
        ..handle = (_) {
          started.complete();
          return reply.future;
        };
      final game = local(core);
      final initializing = game.initialize();
      await started.future;
      game.dispose();
      reply.complete({'ok': true, 'state': core.current});
      await initializing;
      expect(game.state, isNull);
    },
  );

  test(
    'full controller integrates real C++ computer turns and reload persistence',
    () async {
      final game = GameController(
        coreFactory: (dictionary) => createCore(dictionary, seed: 12345),
      );
      await game.initialize();
      expect(game.failed, isFalse);
      await game.act('fill');
      expect(game.state!['rack'], hasLength(8));
      await game.act('hint');
      expect(game.hint, isNotNull);
      await game.act('hint', {'apply': true});
      expect(game.state!['canPlay'], isTrue);
      await game.act('play');
      expect(game.state!['currentPlayer'], 0);
      expect(
        (game.state!['scores'] as List).every((score) => (score as int) > 0),
        isTrue,
      );
      final savedState = jsonEncode(game.state);
      game.dispose();
      final restored = GameController();
      addTearDown(restored.dispose);
      await restored.initialize();
      expect(jsonEncode(restored.state), savedState);
      expect(restored.storageWarning, isNull);
    },
  );

  test('malformed or rejected local saves retain a playable engine', () async {
    for (final save in ['{broken', '{"version":99}']) {
      SharedPreferences.setMockInitialValues({GameController.saveKey: save});
      final core = ScriptedCore()
        ..handle = (command) => command['op'] == 'restore'
            ? {'ok': false, 'state': snapshot()['state']}
            : null;
      final game = local(core);
      await game.initialize();
      expect(game.failed, isFalse);
      expect(game.message, contains('could not be restored'));
      expect(game.state, isNotNull);
      game.dispose();
    }
  });

  test(
    'initialization reports preference, dictionary and native startup failures',
    () async {
      final variants = [
        GameController(
          preferencesLoader: () async => throw StateError('preferences'),
        ),
        GameController(
          dictionaryLoader: () async => throw StateError('dictionary'),
        ),
        GameController(
          dictionaryLoader: () async => 'AT',
          coreFactory: (_) async => throw StateError('native'),
        ),
      ];
      for (final game in variants) {
        await game.initialize();
        expect(game.failed, isTrue);
        expect(game.busy, isFalse);
        expect(game.message, startsWith('Unable to start Jot:'));
        game.dispose();
      }
    },
  );

  test(
    'dispose during dictionary loading never creates a native handle',
    () async {
      final dictionary = Completer<String>();
      final started = Completer<void>();
      var created = false;
      final game = GameController(
        dictionaryLoader: () {
          started.complete();
          return dictionary.future;
        },
        coreFactory: (_) async {
          created = true;
          return ScriptedCore();
        },
      );
      final initializing = game.initialize();
      await started.future;
      game.dispose();
      dictionary.complete('AT');
      await initializing;
      expect(created, isFalse);
    },
  );

  test('dispose during native initialization closes the late handle', () async {
    final native = Completer<CoreTransport>();
    final started = Completer<void>();
    final core = ScriptedCore();
    final game = GameController(
      dictionaryLoader: () async => 'AT',
      coreFactory: (_) {
        started.complete();
        return native.future;
      },
    );
    final initializing = game.initialize();
    await started.future;
    game.dispose();
    native.complete(core);
    await initializing;
    expect(core.closed, isTrue);
  });

  test(
    'local transport exceptions release busy state and preserve presentation',
    () async {
      final core = ScriptedCore();
      final game = local(core);
      addTearDown(game.dispose);
      await game.initialize();
      core.handle = (_) => throw StateError('worker failed');
      await game.act('fill');
      expect(game.busy, isFalse);
      expect(game.message, contains('could not finish'));
      expect(game.state, isNotNull);
    },
  );

  test(
    'invalid native saves and failed disk writes display persistence warnings',
    () async {
      for (final diskThrows in [false, true]) {
        SharedPreferences.setMockInitialValues({});
        final store = FailingPreferencesStore()
          ..fail = true
          ..throws = diskThrows;
        SharedPreferencesStorePlatform.instance = store;
        final game = local(ScriptedCore());
        await game.initialize();
        expect(game.storageWarning, contains('could not be saved'));
        game.dispose();
      }
      SharedPreferences.setMockInitialValues({});
      final game = local(ScriptedCore()..failSave = true);
      addTearDown(game.dispose);
      await game.initialize();
      expect(game.storageWarning, contains('could not be saved'));
    },
  );

  test(
    'concurrent local actions are ignored while a command is pending',
    () async {
      final core = ScriptedCore();
      final game = local(core);
      addTearDown(game.dispose);
      await game.initialize();
      final held = Completer<Map<String, dynamic>>();
      final started = Completer<void>();
      core.handle = (command) {
        started.complete();
        return held.future;
      };
      final first = game.act('fill');
      await started.future;
      await game.act('pass');
      held.complete({
        'ok': false,
        'message': 'Rejected',
        'state': core.current,
      });
      await first;
      expect(
        core.commands.where((command) => command['op'] == 'pass'),
        isEmpty,
      );
      expect(game.busy, isFalse);
    },
  );

  test(
    'reload closes the previous transport even if closing reports an error',
    () async {
      final core = ScriptedCore()..failClose = true;
      final game = local(core);
      addTearDown(game.dispose);
      await game.initialize();
      await game.initialize();
      expect(core.closed, isTrue);
      expect(game.failed, isFalse);
    },
  );
}
