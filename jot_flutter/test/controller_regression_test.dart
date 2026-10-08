import 'dart:async';
import 'dart:convert';

import 'package:flutter_test/flutter_test.dart';
import 'package:jot_flutter/game_controller.dart';
import 'package:jot_flutter/multiplayer_api.dart';
import 'package:shared_preferences/shared_preferences.dart';
import 'package:shared_preferences_platform_interface/shared_preferences_platform_interface.dart';

import 'local_controller_test.dart' show ScriptedCore, FailingPreferencesStore;
import 'support/multiplayer_fixture.dart';

class ScenarioApi extends FakeMultiplayerApi {
  Object? createError;
  Object? readError;
  Object? actionError;
  Map<String, dynamic>? createView;
  Map<String, dynamic>? actionView;
  Completer<Map<String, dynamic>>? heldCreate;
  int reads = 0;
  @override
  Future<Map<String, dynamic>> create(String requestId) async {
    if (heldCreate != null) return heldCreate!.future;
    if (createError != null) throw createError!;
    return createView ?? await super.create(requestId);
  }

  @override
  Future<Map<String, dynamic>> read(String roomId, String token) async {
    reads++;
    if (readError != null) throw readError!;
    return super.read(roomId, token);
  }

  @override
  Future<Map<String, dynamic>> act(
    String roomId,
    String token,
    Map<String, dynamic> request,
  ) async {
    if (actionError != null) throw actionError!;
    return actionView ?? await super.act(roomId, token, request);
  }
}

class HeldPreferencesStore extends InMemorySharedPreferencesStore {
  final String suffix;
  final started = Completer<void>();
  final release = Completer<bool>();
  HeldPreferencesStore(this.suffix) : super.empty();
  @override
  Future<bool> setValue(String type, String key, Object value) async {
    if (key.endsWith(suffix)) {
      if (!started.isCompleted) started.complete();
      if (!await release.future) return false;
    }
    return super.setValue(type, key, value);
  }
}

GameController gameFor(
  ScenarioApi api, {
  Duration poll = const Duration(hours: 1),
}) => GameController(
  multiplayerApi: api,
  initialUri: Uri.parse('https://jot-game.pages.dev/'),
  pollInterval: poll,
  dictionaryLoader: () async => 'AT',
  coreFactory: (_) async => ScriptedCore(),
);

void main() {
  TestWidgetsFlutterBinding.ensureInitialized();
  setUp(
    () => SharedPreferences.setMockInitialValues({
      GameController.onlineSetupKey: jsonEncode({
        'operation': 'create',
        'requestId': 'id',
      }),
    }),
  );

  test(
    'Disposed controllers do not continue after an asynchronous preference load',
    () async {
      final prefs = Completer<SharedPreferences>();
      final game = GameController(preferencesLoader: () => prefs.future);
      final initializing = game.initialize();
      game.dispose();
      prefs.complete(await SharedPreferences.getInstance());
      await initializing;
      expect(game.state, isNull);
    },
  );

  test(
    'All public mode and reconnect entry points ignore work while busy or disposed',
    () async {
      final api = ScenarioApi();
      final game = gameFor(api);
      await game.createOnline();
      await game.returnToLocal();
      await game.resumeOnline();
      await game.retryOnline();
      expect(api.createIds, isEmpty);
      game.dispose();
      game.busy = false;
      await game.createOnline();
      await game.returnToLocal();
      await game.resumeOnline();
      await game.retryOnline();
      expect(api.createIds, isEmpty);
    },
  );

  test(
    'Disposal while saving an invitation prevents creating an orphan server room',
    () async {
      SharedPreferences.setMockInitialValues({});
      await SharedPreferences.getInstance();
      final store = HeldPreferencesStore(GameController.onlineSetupKey);
      SharedPreferencesStorePlatform.instance = store;
      final api = ScenarioApi();
      final game = gameFor(api)..busy = false;
      final creating = game.createOnline();
      await store.started.future;
      game.dispose();
      store.release.complete(true);
      await creating;
      expect(api.createIds, isEmpty);
    },
  );

  test(
    'Disposal while storing a created seat prevents restarting its polling timer',
    () async {
      await SharedPreferences.getInstance();
      final store = HeldPreferencesStore(GameController.onlineSaveKey);
      SharedPreferencesStorePlatform.instance = store;
      final api = ScenarioApi();
      final game = gameFor(api, poll: const Duration(milliseconds: 5));
      final initializing = game.initialize();
      await store.started.future;
      game.dispose();
      store.release.complete(true);
      await initializing;
      await Future<void>.delayed(const Duration(milliseconds: 20));
      expect(api.reads, 0);
    },
  );

  test(
    'Late failing polls cannot change presentation after disposal',
    () async {
      final api = ScenarioApi()..view = snapshot(revision: 1, joined: true);
      final game = gameFor(api);
      await game.initialize();
      api.heldRead = Completer<Map<String, dynamic>>();
      final polling = game.retryOnline();
      game.dispose();
      api.heldRead!.completeError(
        const MultiplayerException('Late error', retryable: true),
      );
      await polling;
      expect(game.connectionError, isNull);
    },
  );

  test(
    'Disposal after a terminal poll failure and before its save prevents stale errors',
    () async {
      final api = ScenarioApi()..view = snapshot(revision: 1, joined: true);
      final game = gameFor(api);
      await game.initialize();
      final store = HeldPreferencesStore(GameController.onlineSaveKey);
      SharedPreferencesStorePlatform.instance = store;
      api.readError = const MultiplayerException('Expired seat');
      final polling = game.retryOnline();
      await store.started.future;
      game.dispose();
      store.release.complete(true);
      await polling;
      expect(game.connectionError, isNull);
    },
  );

  test(
    'A queued action abandoned during an in-flight poll does not reach the server',
    () async {
      final api = ScenarioApi()..view = snapshot(revision: 1, joined: true);
      final game = gameFor(api, poll: const Duration(milliseconds: 5));
      await game.initialize();
      api.heldRead = Completer<Map<String, dynamic>>();
      while (api.reads == 0) {
        await Future<void>.delayed(const Duration(milliseconds: 1));
      }
      final action = game.act('fill');
      await Future<void>.delayed(Duration.zero);
      game.dispose();
      api.heldRead!.complete(snapshot(revision: 1, joined: true));
      await action;
      expect(api.requests, isEmpty);
    },
  );

  test(
    'A seat expiring while a move is being persisted prevents sending that move',
    () async {
      final api = ScenarioApi()..view = snapshot(revision: 1, joined: true);
      final game = gameFor(api, poll: const Duration(milliseconds: 5));
      addTearDown(game.dispose);
      await game.initialize();
      api.heldRead = Completer<Map<String, dynamic>>();
      while (api.reads == 0) {
        await Future<void>.delayed(const Duration(milliseconds: 1));
      }
      final store = HeldPreferencesStore(GameController.onlineSaveKey);
      SharedPreferencesStorePlatform.instance = store;
      final moving = game.act('fill');
      await store.started.future;
      api.heldRead!.completeError(const MultiplayerException('Seat expired'));
      await Future<void>.delayed(Duration.zero);
      store.release.complete(true);
      await moving;
      await Future<void>.delayed(const Duration(milliseconds: 5));
      expect(api.requests, isEmpty);
      expect(game.connected, isFalse);
      expect(game.connectionError, 'Seat expired');
    },
  );

  test(
    'Opening a known invitation resumes its persisted pending command',
    () async {
      final pending = {
        'revision': 1,
        'requestId': 'lost-command',
        'command': {'op': 'fill'},
      };
      SharedPreferences.setMockInitialValues({
        GameController.onlineSaveKey: jsonEncode({
          ...snapshot(revision: 1, joined: true),
          'playerToken': hostToken,
          'pendingAction': pending,
        }),
      });
      final api = ScenarioApi();
      final game = GameController(
        multiplayerApi: api,
        initialUri: Uri.parse(inviteUrl),
        pollInterval: const Duration(hours: 1),
      );
      addTearDown(game.dispose);
      await game.initialize();
      expect(api.requests.single, pending);
      expect(api.joinIds, isEmpty);
    },
  );

  test(
    'Saved invitation nonce survives leaving another room and reopening the invitation',
    () async {
      SharedPreferences.setMockInitialValues({
        GameController.invitationHistoryKey: jsonEncode({
          room: {
            'operation': 'join',
            'roomId': room,
            'inviteToken': inviteToken,
            'requestId': 'persisted-join',
          },
        }),
      });
      final api = ScenarioApi();
      final game = GameController(
        multiplayerApi: api,
        initialUri: Uri.parse(inviteUrl),
        pollInterval: const Duration(hours: 1),
      );
      addTearDown(game.dispose);
      await game.initialize();
      expect(api.joinIds.single, 'persisted-join');
    },
  );

  test(
    'Pasting the current room refreshes it; pasting a saved room resumes its seat',
    () async {
      final api = ScenarioApi()..view = snapshot(revision: 1, joined: true);
      final game = gameFor(api);
      addTearDown(game.dispose);
      await game.initialize();
      await game.joinOnline(inviteUrl);
      expect(api.reads, 1);
      await game.returnToLocal();
      await game.joinOnline(inviteUrl);
      expect(game.isOnline, isTrue);
      expect(api.joinIds, isEmpty);
    },
  );

  test(
    'Resuming from local retries a persisted move identity instead of making a new move',
    () async {
      final pending = {
        'revision': 1,
        'requestId': 'resume-command',
        'command': {'op': 'fill'},
      };
      SharedPreferences.setMockInitialValues({
        GameController.onlineSaveKey: jsonEncode({
          ...snapshot(revision: 1, joined: true),
          'playerToken': hostToken,
          'pendingAction': pending,
        }),
        GameController.localModeKey: true,
      });
      final api = ScenarioApi();
      final game = gameFor(api);
      addTearDown(game.dispose);
      await game.initialize();
      await game.resumeOnline();
      expect(api.requests.single, pending);
    },
  );

  test(
    'Disposal before retry storage completes prevents transmitting a queued action',
    () async {
      final api = ScenarioApi()..view = snapshot(revision: 1, joined: true);
      final game = gameFor(api);
      await game.initialize();
      final action = game.act('fill');
      game.dispose();
      await action;
      expect(api.requests, isEmpty);
    },
  );

  test(
    'Storage failures during manual setup retry release the busy gate',
    () async {
      final api = ScenarioApi()
        ..createError = const MultiplayerException('Offline', retryable: true);
      final game = gameFor(api);
      addTearDown(game.dispose);
      await game.initialize();
      SharedPreferencesStorePlatform.instance = FailingPreferencesStore()
        ..fail = true;
      await game.retryOnline();
      expect(game.message, contains('Enable browser storage'));
      expect(game.busy, isFalse);
      await game.act('fill');
      expect(api.requests, isEmpty);
    },
  );

  test(
    'A failed local engine load after an online game leaves a visible retry path',
    () async {
      final game = GameController(
        multiplayerApi: ScenarioApi(),
        dictionaryLoader: () async => throw StateError('dictionary missing'),
      );
      addTearDown(game.dispose);
      await game.initialize();
      await game.returnToLocal();
      expect(game.failed, isTrue);
      expect(game.busy, isFalse);
      expect(game.message, startsWith('Unable to load your local game:'));
    },
  );

  test(
    'Online rooms survive switching to local and resuming; histories keep only session objects',
    () async {
      final api = ScenarioApi()..view = snapshot(revision: 1, joined: true);
      final game = gameFor(api);
      addTearDown(game.dispose);
      await game.initialize();
      expect(game.savedOnlineGames.single['roomId'], room);
      await game.returnToLocal();
      expect(game.isOnline, isFalse);
      expect(game.canResumeOnline, isTrue);
      expect(game.state, isNotNull);
      await game.resumeOnline();
      expect(game.isOnline, isTrue);
      expect(game.canAct, isTrue);
      final prefs = await SharedPreferences.getInstance();
      await prefs.setString(
        GameController.onlineHistoryKey,
        jsonEncode({
          'good': {...snapshot(), 'playerToken': hostToken},
          'notASession': 'ignored',
        }),
      );
      expect(game.savedOnlineGames, hasLength(1));
    },
  );

  test(
    'A corrupt stored session and corrupt history fall back to a local game',
    () async {
      SharedPreferences.setMockInitialValues({
        GameController.onlineSaveKey: '{broken',
        GameController.onlineHistoryKey: '[]',
      });
      final game = gameFor(ScenarioApi());
      addTearDown(game.dispose);
      await game.initialize();
      expect(game.isOnline, isFalse);
      expect(game.savedOnlineGames, isEmpty);
      await game.resumeOnline('unknown-room');
      expect(game.message, 'This device has no saved online game.');
      await game.joinOnline('not a link');
      expect(game.message, contains('Paste the full Jot invite link'));
    },
  );

  test('Local preference is honored when an online seat is saved', () async {
    SharedPreferences.setMockInitialValues({
      GameController.onlineSaveKey: jsonEncode({
        ...snapshot(),
        'playerToken': hostToken,
      }),
      GameController.localModeKey: true,
    });
    final game = gameFor(ScenarioApi());
    addTearDown(game.dispose);
    await game.initialize();
    expect(game.isOnline, isFalse);
    expect(game.canResumeOnline, isTrue);
    await game.createOnline();
    expect(game.isOnline, isTrue);
  });

  test(
    'Expired seats stop automatic polling and retain manual recovery',
    () async {
      final api = ScenarioApi()..view = snapshot(revision: 1, joined: true);
      final game = gameFor(api, poll: const Duration(milliseconds: 10));
      addTearDown(game.dispose);
      await game.initialize();
      api.readError = const MultiplayerException(
        'This room expired.',
        code: 'room_expired',
      );
      await game.retryOnline();
      final reads = api.reads;
      await Future<void>.delayed(const Duration(milliseconds: 40));
      expect(api.reads, reads);
      expect(game.connectionError, 'This room expired.');
      api.readError = null;
      await game.retryOnline();
      expect(game.connected, isTrue);
    },
  );

  test(
    'Polling refreshes friend moves and preserves unchanged hints',
    () async {
      final api = ScenarioApi()..view = snapshot(revision: 1, joined: true);
      final game = gameFor(api, poll: const Duration(milliseconds: 10));
      addTearDown(game.dispose);
      await game.initialize();
      api.actionView = {
        ...snapshot(revision: 1, joined: true),
        'hint': {'word': 'CAT', 'score': 8},
        'message': 'Hint ready',
        'ok': true,
      };
      await game.act('hint');
      expect(game.hint!['word'], 'CAT');
      await Future<void>.delayed(const Duration(milliseconds: 30));
      expect(game.hint!['word'], 'CAT');
      api.view = snapshot(revision: 2, joined: true);
      (api.view['state'] as Map)['over'] = true;
      await Future<void>.delayed(const Duration(milliseconds: 30));
      expect(game.onlineRevision, 2);
      expect(game.message, 'Game complete.');
      expect(game.hint, isNull);
    },
  );

  test('Lower revisions never roll back a newer snapshot', () async {
    final api = ScenarioApi()..view = snapshot(revision: 5, joined: true);
    final game = gameFor(api);
    addTearDown(game.dispose);
    await game.initialize();
    api.view = snapshot(revision: 1, joined: false);
    await game.retryOnline();
    expect(game.onlineRevision, 5);
    expect(game.joined, isTrue);
  });

  test(
    'Incomplete protocol snapshots and unexpected exceptions are recoverable',
    () async {
      final api = ScenarioApi()..createView = {};
      final game = gameFor(api);
      addTearDown(game.dispose);
      await game.initialize();
      expect(game.failed, isTrue);
      expect(game.message, contains('incomplete game session'));
      api.createView = null;
      await game.createOnline();
      expect(game.isOnline, isTrue);
      api.view = {'revision': 1};
      await game.retryOnline();
      expect(game.connectionError, contains('incomplete game snapshot'));
      api.readError = StateError('Unexpected transport failure');
      await game.retryOnline();
      expect(game.message, 'Unable to reach the game server.');
    },
  );

  test(
    'Online commands are not sent if their retry identity cannot be persisted',
    () async {
      for (final throws in [false, true]) {
        SharedPreferences.setMockInitialValues({
          GameController.onlineSetupKey: jsonEncode({
            'operation': 'create',
            'requestId': 'id',
          }),
        });
        final api = ScenarioApi()..view = snapshot(revision: 1, joined: true);
        final game = gameFor(api);
        await game.initialize();
        SharedPreferencesStorePlatform.instance = FailingPreferencesStore()
          ..fail = true
          ..throws = throws;
        await game.act('fill');
        expect(api.requests, isEmpty);
        expect(game.message, contains('move was not sent'));
        expect(game.storageWarning, contains('could not be saved'));
        game.dispose();
      }
    },
  );

  test(
    'Setup fails clearly when browser storage refuses a pending invitation',
    () async {
      SharedPreferences.setMockInitialValues({});
      SharedPreferencesStorePlatform.instance = FailingPreferencesStore()
        ..fail = true;
      final api = ScenarioApi();
      final game = gameFor(api)..busy = false;
      addTearDown(game.dispose);
      await game.createOnline();
      expect(api.createIds, isEmpty);
      expect(game.message, contains('Enable browser storage'));
      expect(game.busy, isFalse);
    },
  );

  test('Disposal during room creation cannot publish a late session', () async {
    final api = ScenarioApi()..heldCreate = Completer<Map<String, dynamic>>();
    final game = gameFor(api);
    final initializing = game.initialize();
    await Future<void>.delayed(Duration.zero);
    game.dispose();
    api.heldCreate!.complete({...snapshot(), 'playerToken': hostToken});
    await initializing;
    expect(game.isOnline, isFalse);
  });

  test(
    'A gameplay action waits for an in-flight poll and retains its original revision',
    () async {
      final api = ScenarioApi()..view = snapshot(revision: 1, joined: true);
      final game = gameFor(api, poll: const Duration(milliseconds: 10));
      addTearDown(game.dispose);
      await game.initialize();
      api.heldRead = Completer<Map<String, dynamic>>();
      while (api.reads == 0) {
        await Future<void>.delayed(const Duration(milliseconds: 5));
      }
      final action = game.act('fill');
      await Future<void>.delayed(Duration.zero);
      api.heldRead!.complete(snapshot(revision: 1, joined: true));
      await action;
      expect(api.requests.single['revision'], 1);
      expect(game.onlineRevision, 2);
    },
  );
}
