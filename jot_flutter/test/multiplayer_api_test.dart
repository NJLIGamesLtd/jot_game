import 'dart:async';
import 'dart:convert';

import 'package:flutter_test/flutter_test.dart';
import 'package:http/http.dart' as http;
import 'package:http/testing.dart';
import 'package:jot_flutter/multiplayer_api.dart';

import 'support/multiplayer_fixture.dart';

void main() {
  test(
    'API origins honor overrides, web same-origin and native deployment defaults',
    () {
      expect(
        multiplayerApiBase(configured: 'https://custom.example'),
        Uri.parse('https://custom.example'),
      );
      expect(
        multiplayerApiBase(
          web: true,
          currentUri: Uri.parse('https://jot.example/path?room=abc#invite=abc'),
        ),
        Uri.parse('https://jot.example'),
      );
      expect(
        multiplayerApiBase(web: false),
        Uri.parse('https://jot-game.pages.dev'),
      );
    },
  );
  test(
    'HTTP create, join and read use the documented paths and privacy boundaries',
    () async {
      final seen = <http.Request>[];
      final api = HttpMultiplayerApi(
        client: MockClient((request) async {
          seen.add(request);
          return http.Response(
            jsonEncode(snapshot()),
            request.method == 'POST' ? 201 : 200,
          );
        }),
      );
      addTearDown(api.close);
      await api.create('create-id');
      await api.join(room, inviteToken, 'join-id');
      await api.read(room, guestToken);
      expect(seen.map((request) => request.url.path), [
        '/api/rooms',
        '/api/rooms/$room/join',
        '/api/rooms/$room',
      ]);
      expect(seen[0].headers['Authorization'], isNull);
      expect(jsonDecode(seen[0].body), {'requestId': 'create-id'});
      expect(jsonDecode(seen[1].body), {
        'requestId': 'join-id',
        'inviteToken': inviteToken,
      });
      expect(seen[1].url.toString(), isNot(contains(inviteToken)));
      expect(seen[2].headers['Authorization'], 'Bearer $guestToken');
    },
  );

  for (final status in [401, 409, 429, 500]) {
    test(
      'HTTP $status classifies retryability and preserves authoritative conflicts',
      () async {
        final body = {
          'error': 'A server error',
          'code': 'test_error',
          if (status == 409) ...snapshot(revision: 8),
        };
        final api = HttpMultiplayerApi(
          client: MockClient(
            (_) async => http.Response(jsonEncode(body), status),
          ),
        );
        addTearDown(api.close);
        try {
          await api.read(room, hostToken);
          fail('An HTTP error must throw.');
        } on MultiplayerException catch (error) {
          expect(error.toString(), 'A server error');
          expect(error.code, 'test_error');
          expect(error.retryable, status == 429 || status >= 500);
          expect(error.snapshot != null, status == 409);
        }
      },
    );
  }

  test('Missing server error text has a safe fallback', () async {
    final api = HttpMultiplayerApi(
      client: MockClient((_) async => http.Response('{}', 400)),
    );
    addTearDown(api.close);
    await expectLater(
      api.create('id'),
      throwsA(
        isA<MultiplayerException>().having(
          (error) => error.message,
          'message',
          'The game server could not complete this request.',
        ),
      ),
    );
  });

  for (final response in ['{broken', '[]']) {
    test(
      'Malformed response $response reports a retryable connection failure',
      () async {
        final api = HttpMultiplayerApi(
          client: MockClient((_) async => http.Response(response, 200)),
        );
        addTearDown(api.close);
        await expectLater(
          api.read(room, hostToken),
          throwsA(
            isA<MultiplayerException>().having(
              (error) => error.retryable,
              'retryable',
              isTrue,
            ),
          ),
        );
      },
    );
  }

  test('Transport exceptions never expose bearer credentials', () async {
    final api = HttpMultiplayerApi(
      client: MockClient((_) async => throw StateError('Bearer $hostToken')),
    );
    addTearDown(api.close);
    await expectLater(
      api.read(room, hostToken),
      throwsA(
        isA<MultiplayerException>().having(
          (error) => error.message,
          'safe message',
          isNot(contains(hostToken)),
        ),
      ),
    );
  });

  testWidgets(
    'Timeout applies to the complete request including connection establishment',
    (tester) async {
      final held = Completer<http.Response>();
      final api = HttpMultiplayerApi(client: MockClient((_) => held.future));
      addTearDown(api.close);
      final pending = expectLater(
        api.read(room, hostToken),
        throwsA(
          isA<MultiplayerException>().having(
            (error) => error.retryable,
            'retryable',
            isTrue,
          ),
        ),
      );
      await tester.pump(const Duration(seconds: 31));
      await pending;
    },
  );

  test('Request identifiers are unique UUID v4 values', () {
    final identifiers = List.generate(200, (_) => newRequestId());
    expect(identifiers.toSet(), hasLength(200));
    expect(
      identifiers.every(
        (id) => RegExp(
          r'^[0-9a-f]{8}-[0-9a-f]{4}-4[0-9a-f]{3}-[89ab][0-9a-f]{3}-[0-9a-f]{12}$',
        ).hasMatch(id),
      ),
      isTrue,
    );
    expect(GameInvitation.parse('http://['), isNull);
    expect(
      GameInvitation.parse(
        'https://jot-game.pages.dev/#join=$room&invite=$inviteToken',
      )!.roomId,
      room,
    );
  });
}
