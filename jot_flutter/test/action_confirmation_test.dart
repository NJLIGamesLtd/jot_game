import 'dart:async';
import 'dart:convert';

import 'package:flutter_test/flutter_test.dart';
import 'package:jot_core/jot_core.dart';
import 'package:jot_flutter/game_controller.dart';
import 'package:shared_preferences/shared_preferences.dart';
import 'package:shared_preferences_platform_interface/shared_preferences_platform_interface.dart';

import 'support/multiplayer_fixture.dart';

class _ReceiptApi extends FakeMultiplayerApi {
  FutureOr<Map<String, dynamic>> Function(Map<String, dynamic>)? reply;

  @override
  Future<Map<String, dynamic>> act(
    String roomId,
    String playerToken,
    Map<String, dynamic> request,
  ) async {
    if (reply == null) return super.act(roomId, playerToken, request);
    requests.add(Map<String, dynamic>.from(request));
    return await reply!(request);
  }
}

class _ReceiptPreferencesStore extends InMemorySharedPreferencesStore {
  bool fail = false;

  _ReceiptPreferencesStore() : super.empty();

  @override
  Future<bool> setValue(String type, String key, Object value) async =>
      fail ? false : await super.setValue(type, key, value);
}

Future<GameController> _online(
  _ReceiptApi api, {
  bool autoDispose = true,
}) async {
  api.view = snapshot(revision: 1, joined: true);
  SharedPreferences.setMockInitialValues({
    GameController.onlineSetupKey: jsonEncode({
      'operation': 'create',
      'requestId': '00000000-0000-4000-8000-000000000001',
    }),
  });
  final game = controller(api);
  if (autoDispose) addTearDown(game.dispose);
  await game.initialize();
  expect(game.canAct, isTrue);
  return game;
}

Future<Map<String, dynamic>> _storedSession() async {
  final preferences = await SharedPreferences.getInstance();
  return jsonDecode(preferences.getString(GameController.onlineSaveKey)!)
      as Map<String, dynamic>;
}

void main() {
  TestWidgetsFlutterBinding.ensureInitialized();
  setUp(() => SharedPreferences.setMockInitialValues({}));

  test('Local receipts follow actual C++ acceptance and rejection', () async {
    final game = GameController(
      multiplayerApi: FakeMultiplayerApi(),
      dictionaryLoader: () async => 'AT\nCAT',
      coreFactory: (dictionary) => createCore(dictionary, seed: 21, robot: -1),
    );
    var disposed = false;
    addTearDown(() {
      if (!disposed) game.dispose();
    });
    expect(await game.act('fill'), isFalse);
    await game.initialize();
    expect(await game.act('fill'), isTrue);
    expect(game.state!['rack'], hasLength(8));
    final before = jsonDecode(jsonEncode(game.state));
    expect(await game.act('place', {'rack': 100, 'row': 7, 'col': 7}), isFalse);
    expect(game.state, before);
    expect(game.busy, isFalse);
    game.dispose();
    disposed = true;
    expect(await game.act('recall'), isFalse);
  });

  test(
    'An accepted online command returns an authoritative true receipt',
    () async {
      final api = _ReceiptApi();
      final game = await _online(api);
      expect(await game.act('recall'), isTrue);
      expect(api.requests.single['command'], {'op': 'recall'});
      expect(game.onlineRevision, 2);
      expect(game.connected, isTrue);
      expect(game.busy, isFalse);
      expect((await _storedSession())['pendingAction'], isNull);
    },
  );

  test(
    'A rejected command returns false even when its refreshed board allows drawing',
    () async {
      final api = _ReceiptApi();
      final game = await _online(api);
      api.reply = (_) => {
        ...snapshot(revision: 2, joined: true),
        'ok': false,
        'message': 'The command was rejected.',
      };
      expect(await game.act('recall'), isFalse);
      expect(game.onlineRevision, 2);
      expect(game.state!['canDraw'], isTrue);
      expect(game.canAct, isTrue);
      expect(game.message, 'The command was rejected.');
      expect(api.requests, hasLength(1));
      expect((await _storedSession())['pendingAction'], isNull);
    },
  );

  test(
    'A stale revision refreshes the board but does not confirm the command',
    () async {
      final api = _ReceiptApi();
      final game = await _online(api);
      api.conflictNext = true;
      expect(await game.act('recall'), isFalse);
      expect(game.onlineRevision, 3);
      expect(game.connected, isTrue);
      expect(game.canAct, isTrue);
      expect(game.message, contains('another device'));
      expect(api.requests, hasLength(1));
      expect((await _storedSession())['pendingAction'], isNull);
    },
  );

  test(
    'A lost reply returns false and retains the same request for reconnect',
    () async {
      final api = _ReceiptApi();
      final game = await _online(api);
      api.loseNextAction = true;
      expect(await game.act('recall'), isFalse);
      expect(game.connected, isFalse);
      expect(game.canAct, isFalse);
      final request = api.requests.single;
      expect((await _storedSession())['pendingAction'], request);
      await game.retryOnline();
      expect(api.requests, [request, request]);
      expect(game.connected, isTrue);
      expect(game.canAct, isTrue);
      expect((await _storedSession())['pendingAction'], isNull);
    },
  );

  test(
    'A failed pre-send save returns false without sending a command',
    () async {
      final api = _ReceiptApi();
      final game = await _online(api);
      final before = jsonDecode(jsonEncode(game.state));
      final originalStore = SharedPreferencesStorePlatform.instance;
      addTearDown(
        () => SharedPreferencesStorePlatform.instance = originalStore,
      );
      SharedPreferencesStorePlatform.instance = _ReceiptPreferencesStore()
        ..fail = true;
      expect(await game.act('recall'), isFalse);
      expect(api.requests, isEmpty);
      expect(game.state, before);
      expect(game.canAct, isTrue);
      expect(game.busy, isFalse);
      expect(game.storageWarning, isNotNull);
      expect(game.message, contains('not sent'));
    },
  );

  test(
    'A post-send storage failure preserves the accepted server receipt',
    () async {
      final api = _ReceiptApi();
      final game = await _online(api);
      final originalStore = SharedPreferencesStorePlatform.instance;
      addTearDown(
        () => SharedPreferencesStorePlatform.instance = originalStore,
      );
      final store = _ReceiptPreferencesStore();
      SharedPreferencesStorePlatform.instance = store;
      api.reply = (_) {
        store.fail = true;
        return {
          ...snapshot(revision: 2, joined: true),
          'ok': true,
          'message': 'Tiles returned.',
        };
      };
      expect(await game.act('recall'), isTrue);
      expect(api.requests, hasLength(1));
      expect(game.onlineRevision, 2);
      expect(game.connected, isTrue);
      expect(game.storageWarning, isNotNull);
      expect(game.busy, isFalse);
    },
  );

  test(
    'An accepted reply arriving after disposal cannot confirm follow-up work',
    () async {
      final api = _ReceiptApi();
      final game = await _online(api, autoDispose: false);
      var disposed = false;
      addTearDown(() {
        if (!disposed) game.dispose();
      });
      final started = Completer<void>();
      final response = Completer<Map<String, dynamic>>();
      api.reply = (_) {
        started.complete();
        return response.future;
      };
      final action = game.act('recall');
      await started.future;
      game.dispose();
      disposed = true;
      response.complete({
        ...snapshot(revision: 2, joined: true),
        'ok': true,
        'message': 'Tiles returned.',
      });
      expect(await action, isFalse);
      expect(game.onlineRevision, 1);
    },
  );
}
