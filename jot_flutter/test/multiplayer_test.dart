import 'dart:async';
import 'dart:convert';

import 'package:flutter_test/flutter_test.dart';
import 'package:flutter/services.dart';
import 'package:http/http.dart' as http;
import 'package:http/testing.dart';
import 'package:jot_flutter/game_controller.dart';
import 'package:jot_flutter/browser_location.dart';
import 'package:jot_flutter/multiplayer_api.dart';
import 'package:shared_preferences/shared_preferences.dart';

import 'support/multiplayer_fixture.dart';

void main() {
  TestWidgetsFlutterBinding.ensureInitialized();
  setUp(() {
    SharedPreferences.setMockInitialValues({
      GameController.onlineSetupKey: jsonEncode({
        'operation': 'create',
        'requestId': '00000000-0000-4000-8000-000000000000',
      }),
    });
  });

  test(
    'Invite parser accepts share links and never accepts a bearer token in query',
    () {
      final invitation = GameInvitation.parse(inviteUrl)!;
      expect(invitation.roomId, room);
      expect(invitation.inviteToken, inviteToken);
      expect(
        GameInvitation.parse(
          'https://jot-game.pages.dev/?room=$room&invite=$hostToken',
        ),
        isNull,
      );
      expect(
        GameInvitation.parse(
          'https://jot-game.pages.dev/?room=bad#invite=$inviteToken',
        ),
        isNull,
      );
    },
  );

  test(
    'Waiting rooms cannot send gameplay; joining enables only your own turn',
    () async {
      final api = FakeMultiplayerApi();
      final game = controller(api);
      addTearDown(game.dispose);
      await game.initialize();
      expect(game.isOnline, isTrue);
      expect(game.inviteUrl, isNot(contains(hostToken)));
      expect(game.canAct, isFalse);
      await game.act('fill');
      expect(api.requests, isEmpty);
      api.view = snapshot(revision: 1, joined: true);
      await game.retryOnline();
      expect(game.canAct, isTrue);
      api.view = snapshot(revision: 2, joined: true, player: 0);
      (api.view['state'] as Map)['currentPlayer'] = 1;
      await game.retryOnline();
      expect(game.canAct, isFalse);
      expect((game.state!['rack'] as List).single['letter'], 'A');
    },
  );

  test(
    'A lost move response survives reload and retries the same request once',
    () async {
      final api = FakeMultiplayerApi()
        ..view = snapshot(revision: 1, joined: true);
      final first = controller(api);
      await first.initialize();
      api.loseNextAction = true;
      await first.act('fill');
      expect(first.connected, isFalse);
      expect(first.canAct, isFalse);
      final prefs = await SharedPreferences.getInstance();
      final saved =
          jsonDecode(prefs.getString(GameController.onlineSaveKey)!) as Map;
      expect(
        saved['pendingAction']['requestId'],
        api.requests.single['requestId'],
      );
      first.dispose();
      final resumed = controller(api);
      addTearDown(resumed.dispose);
      await resumed.initialize();
      expect(api.requests.length, 2);
      expect(api.requests[1], api.requests[0]);
      expect(resumed.onlineRevision, 2);
      expect(resumed.connected, isTrue);
      expect(
        (jsonDecode(prefs.getString(GameController.onlineSaveKey)!)
            as Map)['pendingAction'],
        isNull,
      );
    },
  );

  test(
    'A lost join response reuses the persisted nonce after reload',
    () async {
      SharedPreferences.setMockInitialValues({});
      final api = FakeMultiplayerApi()..loseNextJoin = true;
      final first = controller(api, uri: Uri.parse(inviteUrl));
      await first.initialize();
      expect(first.joiningOnline, isTrue);
      first.dispose();
      final resumed = controller(api, uri: Uri.parse(inviteUrl));
      addTearDown(resumed.dispose);
      await resumed.initialize();
      expect(api.joinIds.length, 2);
      expect(api.joinIds[1], api.joinIds[0]);
      expect(resumed.yourPlayer, 1);
      expect(resumed.canAct, isFalse);
      expect((resumed.state!['rack'] as List).single['letter'], 'Z');
      expect(resumed.inviteUrl, isNull);
    },
  );

  test(
    'Pasting the same invite after a lost join response keeps its nonce',
    () async {
      SharedPreferences.setMockInitialValues({});
      final api = FakeMultiplayerApi()..loseNextJoin = true;
      final game = controller(api, uri: Uri.parse(inviteUrl));
      addTearDown(game.dispose);
      await game.initialize();
      await game.joinOnline(inviteUrl);
      expect(api.joinIds.length, 2);
      expect(api.joinIds[1], api.joinIds[0]);
      expect(game.yourPlayer, 1);
    },
  );

  test(
    'Reopening an old room invite restores the saved seat instead of joining again',
    () async {
      final saved = {
        ...snapshot(revision: 1, joined: true),
        'playerToken': hostToken,
      };
      SharedPreferences.setMockInitialValues({
        GameController.onlineHistoryKey: jsonEncode({room: saved}),
        GameController.localModeKey: true,
      });
      final api = FakeMultiplayerApi()
        ..view = snapshot(revision: 1, joined: true);
      final game = controller(api, uri: Uri.parse(inviteUrl));
      addTearDown(game.dispose);
      await game.initialize();
      expect(game.yourPlayer, 0);
      expect(game.roomId, room);
      expect(api.joinIds, isEmpty);
      expect(game.canAct, isTrue);
    },
  );

  test(
    'A successful reconnect restores the turn message even at the same revision',
    () async {
      final api = FakeMultiplayerApi()
        ..view = snapshot(revision: 1, joined: true);
      final game = controller(api);
      addTearDown(game.dispose);
      await game.initialize();
      api.heldRead = Completer<Map<String, dynamic>>();
      final failedRead = game.retryOnline();
      api.heldRead!.completeError(
        const MultiplayerException('Connection interrupted.', retryable: true),
      );
      await failedRead;
      expect(game.message, 'Connection interrupted.');
      api.heldRead = null;
      await game.retryOnline();
      expect(game.message, 'Your turn. Draw tiles or place your next word.');
      expect(game.connectionError, isNull);
    },
  );

  test(
    'Resuming an existing room supersedes pending setup while keeping its retry nonce',
    () async {
      final api = FakeMultiplayerApi()
        ..view = snapshot(revision: 1, joined: true);
      final game = controller(api);
      addTearDown(game.dispose);
      await game.initialize();
      const otherRoom = 'abcdef0123456789abcdef0123456789';
      final otherInvite =
          'https://jot-game.pages.dev/?room=$otherRoom#invite=$inviteToken';
      api.loseNextJoin = true;
      await game.joinOnline(otherInvite);
      final pendingNonce = api.joinIds.single;
      await game.resumeOnline(room);
      final preferences = await SharedPreferences.getInstance();
      expect(preferences.getString(GameController.onlineSetupKey), isNull);
      expect(game.roomId, room);
      await game.joinOnline(otherInvite);
      expect(api.joinIds[1], pendingNonce);
    },
  );

  test(
    'Processed invitations leave the address bar while preserving unrelated query data',
    () async {
      final calls = <MethodCall>[];
      TestDefaultBinaryMessengerBinding.instance.defaultBinaryMessenger
          .setMockMethodCallHandler(SystemChannels.navigation, (call) async {
            calls.add(call);
            return null;
          });
      addTearDown(
        () => TestDefaultBinaryMessengerBinding.instance.defaultBinaryMessenger
            .setMockMethodCallHandler(SystemChannels.navigation, null),
      );
      await clearInvitationLocation(
        Uri.parse(
          'https://jot-game.pages.dev/?campaign=friend&room=$room#invite=$inviteToken',
        ),
      );
      expect(calls.single.method, 'routeInformationUpdated');
      expect((calls.single.arguments as Map)['replace'], isTrue);
      final cleaned = Uri.parse(
        (calls.single.arguments as Map)['uri'] as String,
      );
      expect(cleaned.queryParameters, {'campaign': 'friend'});
      expect(cleaned.fragment, isEmpty);
    },
  );

  test(
    'Conflicting revisions refresh the snapshot without reapplying the command',
    () async {
      final api = FakeMultiplayerApi()
        ..view = snapshot(revision: 1, joined: true);
      final game = controller(api);
      addTearDown(game.dispose);
      await game.initialize();
      api.conflictNext = true;
      await game.act('pass');
      expect(game.onlineRevision, 3);
      expect(api.requests.length, 1);
      await game.retryOnline();
      expect(api.requests.length, 1);
      expect(game.canAct, isTrue);
    },
  );

  test(
    'Disposed controllers ignore late replies and do not restart polling',
    () async {
      final api = FakeMultiplayerApi();
      final game = controller(api, poll: const Duration(milliseconds: 5));
      await game.initialize();
      api.heldRead = Completer<Map<String, dynamic>>();
      final request = game.retryOnline();
      game.dispose();
      api.heldRead!.complete(snapshot(revision: 9, joined: true));
      await request;
      expect(game.onlineRevision, 0);
    },
  );

  test(
    'HTTP client keeps seat credentials in authorization and sends retry identity',
    () async {
      final api = HttpMultiplayerApi(
        base: Uri.parse('https://jot-game.pages.dev'),
        client: MockClient((request) async {
          expect(request.url.path, '/api/rooms/$room/actions');
          expect(request.url.query, isEmpty);
          expect(request.headers['Authorization'], 'Bearer $hostToken');
          expect(jsonDecode(request.body)['requestId'], 'test-request');
          return http.Response(
            jsonEncode(snapshot(revision: 2, joined: true)),
            200,
          );
        }),
      );
      addTearDown(api.close);
      await api.act(room, hostToken, {
        'revision': 1,
        'requestId': 'test-request',
        'command': {'op': 'fill'},
      });
    },
  );
}
