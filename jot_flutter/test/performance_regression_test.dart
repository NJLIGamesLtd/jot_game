import 'dart:async';
import 'dart:convert';

import 'package:flutter/material.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:jot_core/jot_core.dart';
import 'package:jot_flutter/game_controller.dart';
import 'package:jot_flutter/main.dart';
import 'package:jot_flutter/multiplayer_api.dart';
import 'package:shared_preferences/shared_preferences.dart';
import 'package:shared_preferences_platform_interface/shared_preferences_platform_interface.dart';

import 'support/multiplayer_fixture.dart';

class CountingPreferencesStore extends InMemorySharedPreferencesStore {
  int writes = 0;
  bool fail = false;
  CountingPreferencesStore() : super.empty();

  @override
  Future<bool> setValue(String type, String key, Object value) async {
    writes++;
    return fail ? false : super.setValue(type, key, value);
  }
}

class CountingApi extends FakeMultiplayerApi {
  int reads = 0;
  int activeReads = 0;
  int maximumActiveReads = 0;
  Object? readError;
  Object? createError;
  Completer<Map<String, dynamic>>? heldAction;
  final List<Object?> durableRequests = [];

  @override
  Future<Map<String, dynamic>> create(String requestId) async {
    if (createError != null) throw createError!;
    return super.create(requestId);
  }

  @override
  Future<Map<String, dynamic>> read(String roomId, String playerToken) async {
    reads++;
    activeReads++;
    if (activeReads > maximumActiveReads) maximumActiveReads = activeReads;
    try {
      if (readError != null) throw readError!;
      return await super.read(roomId, playerToken);
    } finally {
      activeReads--;
    }
  }

  @override
  Future<Map<String, dynamic>> act(
    String roomId,
    String playerToken,
    Map<String, dynamic> request,
  ) async {
    final preferences = await SharedPreferences.getInstance();
    final saved =
        jsonDecode(preferences.getString(GameController.onlineSaveKey)!) as Map;
    durableRequests.add(saved['pendingAction']);
    if (heldAction == null) return super.act(roomId, playerToken, request);
    requests.add(Map<String, dynamic>.from(request));
    return heldAction!.future;
  }
}

class CountingCore implements CoreTransport {
  final List<String> commands = [];
  Map<String, dynamic> current = Map<String, dynamic>.from(
    snapshot()['state'] as Map,
  )..['robotPlayer'] = 1;
  int closes = 0;
  bool failClose = false;
  Completer<void>? heldClose;

  @override
  Future<String> execute(String command) async {
    final decoded = jsonDecode(command) as Map;
    final op = decoded['op'] as String;
    commands.add(op);
    if (op == 'save') {
      return jsonEncode({
        'ok': true,
        'save': {'state': current},
      });
    }
    if (op == 'restore') {
      current = Map<String, dynamic>.from(decoded['save']['state'] as Map);
    } else if (op == 'play') {
      current['currentPlayer'] = 1;
    } else if (op == 'robot') {
      current['currentPlayer'] = 0;
    }
    return jsonEncode({'ok': true, 'state': current, 'message': 'Done.'});
  }

  @override
  Future<void> close() async {
    closes++;
    if (heldClose != null) await heldClose!.future;
    if (failClose) throw StateError('close failed');
  }
}

Future<({GameController game, CountingApi api, CountingPreferencesStore store})>
onlineFixture() async {
  SharedPreferences.setMockInitialValues({
    GameController.onlineSetupKey: jsonEncode({
      'operation': 'create',
      'requestId': '00000000-0000-4000-8000-000000000000',
    }),
  });
  await SharedPreferences.getInstance();
  final store = CountingPreferencesStore();
  SharedPreferencesStorePlatform.instance = store;
  final api = CountingApi()..view = snapshot(revision: 1, joined: true);
  final game = controller(api, poll: const Duration(seconds: 2));
  await game.initialize();
  expect(game.canAct, isTrue);
  store.writes = 0;
  return (game: game, api: api, store: store);
}

void main() {
  testWidgets(
    'One unchanged visible minute has no redundant storage or UI work',
    (tester) async {
      final (:game, :api, :store) = await onlineFixture();
      var notifications = 0;
      game.addListener(() => notifications++);
      final state = game.state;
      game.hint = {'word': 'AT'};
      try {
        for (var second = 0; second < 60; second += 2) {
          await tester.pump(const Duration(seconds: 2));
        }
        // This fixture measured 30 reads / 60 writes / 30 notifications before
        // optimization. Count actual work rather than asserting wall-clock speed.
        expect(api.reads, 30);
        expect(store.writes, 0);
        expect(notifications, 0);
        expect(identical(game.state, state), isTrue);
        expect(game.hint!['word'], 'AT');
      } finally {
        game.dispose();
      }
    },
  );

  testWidgets('One hidden minute does no polling, writing or notifying', (
    tester,
  ) async {
    final (:game, :api, :store) = await onlineFixture();
    var notifications = 0;
    game.addListener(() => notifications++);
    try {
      game.setAppVisible(false);
      game.setAppVisible(false);
      for (var second = 0; second < 60; second += 2) {
        await tester.pump(const Duration(seconds: 2));
      }
      expect(api.reads, 0);
      expect(store.writes, 0);
      expect(notifications, 0);
      game.setAppVisible(true);
      await tester.pump();
      expect(api.reads, 1, reason: 'Resume refreshes immediately.');
      game.setAppVisible(true);
      await tester.pump();
      expect(api.reads, 1);
      await tester.pump(const Duration(seconds: 2));
      expect(api.reads, 2);
      expect(store.writes, 0);
      expect(notifications, 0);
    } finally {
      game.dispose();
    }
    game.setAppVisible(false);
    game.setAppVisible(true);
    await tester.pump(const Duration(seconds: 2));
    expect(api.reads, 2);
  });

  testWidgets('A changed friend move persists and notifies exactly once', (
    tester,
  ) async {
    final (:game, :api, :store) = await onlineFixture();
    var notifications = 0;
    game.addListener(() => notifications++);
    try {
      api.view = snapshot(revision: 2, joined: true);
      (api.view['state'] as Map)['currentPlayer'] = 1;
      await tester.pump(const Duration(seconds: 2));
      expect(game.onlineRevision, 2);
      expect(game.canAct, isFalse);
      expect(game.message, 'Waiting for your friend’s turn.');
      expect(store.writes, 2);
      expect(notifications, 1);
      await tester.pump(const Duration(seconds: 2));
      expect(store.writes, 2);
      expect(notifications, 1);
    } finally {
      game.dispose();
    }
  });

  testWidgets(
    'A room initialized while hidden starts polling only when shown',
    (tester) async {
      SharedPreferences.setMockInitialValues({
        GameController.onlineSetupKey: jsonEncode({
          'operation': 'create',
          'requestId': '00000000-0000-4000-8000-000000000000',
        }),
      });
      final api = CountingApi();
      final game = controller(api, poll: const Duration(seconds: 2));
      try {
        game.setAppVisible(false);
        await game.initialize();
        await tester.pump(const Duration(minutes: 1));
        expect(api.reads, 0);
        game.setAppVisible(true);
        await tester.pump();
        expect(api.reads, 1);
      } finally {
        game.dispose();
      }
    },
  );

  testWidgets('Unchanged revisions still persist changed room metadata', (
    tester,
  ) async {
    final (:game, :api, :store) = await onlineFixture();
    var notifications = 0;
    game.addListener(() => notifications++);
    try {
      api.view = snapshot(revision: 1, joined: false)
        ..['inviteUrl'] = '$inviteUrl&campaign=friend';
      await tester.pump(const Duration(seconds: 2));
      expect(game.joined, isFalse);
      expect(game.inviteUrl, api.view['inviteUrl']);
      expect(store.writes, 2);
      expect(notifications, 1);
      await tester.pump(const Duration(seconds: 2));
      expect(store.writes, 2);
      expect(notifications, 1);
    } finally {
      game.dispose();
    }
  });

  testWidgets('Failed persistence retries even an unchanged room snapshot', (
    tester,
  ) async {
    final (:game, :api, :store) = await onlineFixture();
    try {
      store.fail = true;
      api.view = snapshot(revision: 2, joined: true);
      await tester.pump(const Duration(seconds: 2));
      expect(game.storageWarning, isNotNull);
      expect(store.writes, 1);
      store.fail = false;
      var notifications = 0;
      game.addListener(() => notifications++);
      await tester.pump(const Duration(seconds: 2));
      expect(game.storageWarning, isNull);
      expect(store.writes, 3);
      expect(notifications, 1);
      await tester.pump(const Duration(seconds: 2));
      expect(store.writes, 3);
      expect(notifications, 1);
    } finally {
      game.dispose();
    }
  });

  testWidgets(
    'Reconnect at an unchanged revision restores UI without disk writes',
    (tester) async {
      final (:game, :api, :store) = await onlineFixture();
      try {
        api.readError = const MultiplayerException('Offline', retryable: true);
        await tester.pump(const Duration(seconds: 2));
        expect(game.connected, isFalse);
        expect(store.writes, 0);
        api.readError = null;
        var notifications = 0;
        game.addListener(() => notifications++);
        await tester.pump(const Duration(seconds: 2));
        expect(game.connected, isTrue);
        expect(game.connectionError, isNull);
        expect(game.message, 'Your turn. Draw tiles or place your next word.');
        expect(store.writes, 0);
        expect(notifications, 1);
      } finally {
        game.dispose();
      }
    },
  );

  testWidgets(
    'Resume waits for an old read then refreshes without overlapping it',
    (tester) async {
      final (:game, :api, :store) = await onlineFixture();
      try {
        final held = Completer<Map<String, dynamic>>();
        api.heldRead = held;
        await tester.pump(const Duration(seconds: 2));
        expect(api.reads, 1);
        game.setAppVisible(false);
        game.setAppVisible(true);
        await tester.pump();
        expect(api.reads, 1);
        api.heldRead = null;
        api.view = snapshot(revision: 3, joined: true);
        held.complete(snapshot(revision: 1, joined: true));
        await tester.pump();
        expect(api.reads, 2);
        expect(api.maximumActiveReads, 1);
        expect(game.onlineRevision, 3);
        expect(store.writes, 2);
      } finally {
        game.dispose();
      }
    },
  );

  testWidgets(
    'An in-flight read may finish hidden but cannot restart polling',
    (tester) async {
      final (:game, :api, :store) = await onlineFixture();
      try {
        final held = Completer<Map<String, dynamic>>();
        api.heldRead = held;
        await tester.pump(const Duration(seconds: 2));
        game.setAppVisible(false);
        held.complete(snapshot(revision: 2, joined: true));
        await tester.pump();
        expect(game.onlineRevision, 2);
        expect(store.writes, 2);
        await tester.pump(const Duration(minutes: 1));
        expect(api.reads, 1);
      } finally {
        game.dispose();
      }
    },
  );

  testWidgets('Resume defers refresh until a durable action has completed', (
    tester,
  ) async {
    final (:game, :api, :store) = await onlineFixture();
    try {
      final held = Completer<Map<String, dynamic>>();
      api.heldAction = held;
      final action = game.act('fill');
      await tester.pump();
      expect(api.requests, hasLength(1), reason: game.message);
      expect(api.durableRequests.single, api.requests.single);
      game.setAppVisible(false);
      game.setAppVisible(true);
      await tester.pump(const Duration(seconds: 2));
      expect(api.reads, 0);
      api.heldAction = null;
      api.view = snapshot(revision: 2, joined: true);
      held.complete({...api.view, 'ok': true});
      await action;
      await tester.pump();
      expect(api.reads, 1);
      expect(api.requests, hasLength(1));
      expect(store.writes, 4);
      expect(game.busy, isFalse);
    } finally {
      game.dispose();
    }
  });

  testWidgets(
    'A move lost while hidden resumes with the exact durable identity',
    (tester) async {
      final (:game, :api, :store) = await onlineFixture();
      try {
        final held = Completer<Map<String, dynamic>>();
        api.heldAction = held;
        final action = game.act('fill');
        await tester.pump();
        final request = api.requests.single;
        game.setAppVisible(false);
        held.completeError(
          const MultiplayerException('Lost reply', retryable: true),
        );
        expect(await action, isFalse);
        await tester.pump(const Duration(minutes: 1));
        expect(api.requests, hasLength(1));
        expect(api.reads, 0);
        api.heldAction = null;
        game.setAppVisible(true);
        await tester.pump();
        expect(api.requests, [request, request]);
        expect(api.durableRequests, [request, request]);
        expect(api.reads, 0);
        expect(game.connected, isTrue);
        expect(store.writes, 4);
      } finally {
        game.dispose();
      }
    },
  );

  testWidgets(
    'An accepted same-revision action still clears its durable receipt',
    (tester) async {
      final (:game, :api, :store) = await onlineFixture();
      try {
        final held = Completer<Map<String, dynamic>>();
        api.heldAction = held;
        final action = game.act('hint');
        await tester.pump();
        held.complete({
          ...api.view,
          'ok': true,
          'hint': {'word': 'AT'},
        });
        expect(await action, isTrue);
        expect(api.durableRequests.single, api.requests.single);
        expect(game.hint!['word'], 'AT');
        expect(store.writes, 4);
        final preferences = await SharedPreferences.getInstance();
        final saved =
            jsonDecode(preferences.getString(GameController.onlineSaveKey)!)
                as Map;
        expect(saved['pendingAction'], isNull);
      } finally {
        game.dispose();
      }
    },
  );

  testWidgets('Finished rooms stop polling and do not refresh on resume', (
    tester,
  ) async {
    final (:game, :api, :store) = await onlineFixture();
    try {
      api.view = snapshot(revision: 2, joined: true);
      (api.view['state'] as Map)['over'] = true;
      await tester.pump(const Duration(seconds: 2));
      expect(game.message, 'Game complete.');
      expect(api.reads, 1);
      expect(store.writes, 2);
      game.setAppVisible(false);
      game.setAppVisible(true);
      await tester.pump(const Duration(minutes: 1));
      expect(api.reads, 1);
      expect(store.writes, 2);
    } finally {
      game.dispose();
    }
  });

  testWidgets('Terminal failures do not auto-retry when the view resumes', (
    tester,
  ) async {
    final (:game, :api, store: _) = await onlineFixture();
    try {
      api.readError = const MultiplayerException('Expired room');
      await tester.pump(const Duration(seconds: 2));
      game.setAppVisible(false);
      game.setAppVisible(true);
      await tester.pump(const Duration(minutes: 1));
      expect(api.reads, 1);
      expect(game.connectionError, 'Expired room');
    } finally {
      game.dispose();
    }
  });

  testWidgets(
    'GamePage lifecycle pauses hidden views, keeps inactive views fresh',
    (tester) async {
      tester.binding.handleAppLifecycleStateChanged(AppLifecycleState.resumed);
      final (:game, :api, :store) = await onlineFixture();
      await tester.pumpWidget(
        MaterialApp(home: GamePage(controller: game, initialize: false)),
      );
      tester.binding.handleAppLifecycleStateChanged(AppLifecycleState.inactive);
      await tester.pump(const Duration(seconds: 2));
      expect(api.reads, 1);
      tester.binding.handleAppLifecycleStateChanged(AppLifecycleState.hidden);
      tester.binding.handleAppLifecycleStateChanged(AppLifecycleState.paused);
      await tester.pump(const Duration(minutes: 1));
      expect(api.reads, 1);
      expect(store.writes, 0);
      tester.binding.handleAppLifecycleStateChanged(AppLifecycleState.hidden);
      tester.binding.handleAppLifecycleStateChanged(AppLifecycleState.inactive);
      await tester.pump();
      expect(api.reads, 2);
      tester.binding.handleAppLifecycleStateChanged(AppLifecycleState.resumed);
      await tester.pump();
      expect(api.reads, 2);
      tester.binding.handleAppLifecycleStateChanged(AppLifecycleState.inactive);
      tester.binding.handleAppLifecycleStateChanged(AppLifecycleState.hidden);
      tester.binding.handleAppLifecycleStateChanged(AppLifecycleState.paused);
      tester.binding.handleAppLifecycleStateChanged(AppLifecycleState.detached);
      await tester.pump(const Duration(minutes: 1));
      expect(api.reads, 2);
      tester.binding.handleAppLifecycleStateChanged(AppLifecycleState.resumed);
      await tester.pump();
      expect(api.reads, 3);
      await tester.pumpWidget(const SizedBox());
      tester.binding.handleAppLifecycleStateChanged(AppLifecycleState.inactive);
      tester.binding.handleAppLifecycleStateChanged(AppLifecycleState.hidden);
      tester.binding.handleAppLifecycleStateChanged(AppLifecycleState.paused);
      tester.binding.handleAppLifecycleStateChanged(AppLifecycleState.detached);
      await tester.pump(const Duration(minutes: 1));
      expect(api.reads, 3, reason: 'Disposed listeners cannot revive a room.');
      tester.binding.handleAppLifecycleStateChanged(AppLifecycleState.resumed);
      expect(tester.takeException(), isNull);
    },
  );

  test(
    'Local rack actions save once; computer turns keep both durability points',
    () async {
      SharedPreferences.setMockInitialValues({});
      final core = CountingCore();
      final game = GameController(
        multiplayerApi: CountingApi(),
        dictionaryLoader: () async => 'AT',
        coreFactory: (_) async => core,
      );
      try {
        await game.initialize();
        core.commands.clear();
        await game.act('fill');
        expect(core.commands, ['fill', 'save']);
        core.commands.clear();
        await game.act('play');
        expect(core.commands, ['play', 'save', 'robot', 'save']);
        expect(game.state!['currentPlayer'], 0);
        game.setAppVisible(false);
        game.setAppVisible(true);
        expect(core.commands, ['play', 'save', 'robot', 'save']);
      } finally {
        game.dispose();
      }
    },
  );

  test(
    'Successful create/join release local cores and returning restores the save',
    () async {
      for (final join in [false, true]) {
        SharedPreferences.setMockInitialValues({});
        final cores = <CountingCore>[];
        final api = CountingApi();
        final game = GameController(
          multiplayerApi: api,
          pollInterval: const Duration(hours: 1),
          dictionaryLoader: () async => 'AT',
          coreFactory: (_) async {
            final core = CountingCore();
            cores.add(core);
            return core;
          },
        );
        try {
          await game.initialize();
          final localState = jsonEncode(game.state);
          if (join) {
            await game.joinOnline(inviteUrl);
          } else {
            await game.createOnline();
          }
          expect(game.isOnline, isTrue);
          expect(cores.single.closes, 1);
          await game.returnToLocal();
          expect(cores, hasLength(2));
          expect(cores.last.commands.first, 'restore');
          expect(jsonEncode(game.state), localState);
          await game.resumeOnline();
          expect(cores.last.closes, 1);
          expect(game.isOnline, isTrue);
        } finally {
          game.dispose();
        }
        expect(cores.map((core) => core.closes), [1, 1]);
      }
    },
  );

  test(
    'Failed online setup keeps the local engine until the setup succeeds',
    () async {
      SharedPreferences.setMockInitialValues({});
      final core = CountingCore()..failClose = true;
      final api = CountingApi()
        ..createError = const MultiplayerException('Offline', retryable: true);
      final game = GameController(
        multiplayerApi: api,
        dictionaryLoader: () async => 'AT',
        coreFactory: (_) async => core,
        pollInterval: const Duration(hours: 1),
      );
      try {
        await game.initialize();
        await game.createOnline();
        expect(core.closes, 0);
        api.createError = null;
        await game.retryOnline();
        expect(core.closes, 1);
        expect(game.isOnline, isTrue);
        expect(game.connected, isTrue);
      } finally {
        game.dispose();
      }
      expect(core.closes, 1);
    },
  );

  test(
    'Disposal while releasing a core does not reload an obsolete dictionary',
    () async {
      SharedPreferences.setMockInitialValues({});
      final core = CountingCore();
      var dictionaryLoads = 0;
      final game = GameController(
        multiplayerApi: CountingApi(),
        dictionaryLoader: () async {
          dictionaryLoads++;
          return 'AT';
        },
        coreFactory: (_) async => core,
      );
      await game.initialize();
      core.heldClose = Completer<void>();
      final initializing = game.initialize();
      while (core.closes == 0) {
        await Future<void>.delayed(Duration.zero);
      }
      game.dispose();
      core.heldClose!.complete();
      await initializing;
      expect(dictionaryLoads, 1);
      expect(core.closes, 1);
    },
  );

  test(
    'Disposal while releasing a newly online core stops the mode transition',
    () async {
      SharedPreferences.setMockInitialValues({});
      final core = CountingCore();
      final game = GameController(
        multiplayerApi: CountingApi(),
        dictionaryLoader: () async => 'AT',
        coreFactory: (_) async => core,
      );
      await game.initialize();
      core.heldClose = Completer<void>();
      final creating = game.createOnline();
      while (core.closes == 0) {
        await Future<void>.delayed(Duration.zero);
      }
      game.dispose();
      core.heldClose!.complete();
      await creating;
      expect(core.closes, 1);
    },
  );
}
