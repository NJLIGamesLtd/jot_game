import 'dart:async';
import 'package:flutter_test/flutter_test.dart';
import 'package:jot_flutter/game_controller.dart';
import 'package:jot_flutter/multiplayer_api.dart';

const room = '0123456789abcdef0123456789abcdef';
final hostToken = List.filled(64, 'a').join();
final inviteToken = List.filled(64, 'b').join();
final guestToken = List.filled(64, 'c').join();
final inviteUrl = 'https://jot-game.pages.dev/?room=$room#invite=$inviteToken';

Map<String, dynamic> snapshot({
  int revision = 0,
  bool joined = false,
  int player = 0,
}) => {
  'roomId': room,
  'yourPlayer': player,
  'revision': revision,
  'joined': joined,
  if (player == 0) 'inviteUrl': inviteUrl,
  'state': {
    'currentPlayer': 0,
    'robotPlayer': -1,
    'over': false,
    'rack': [
      {
        'letter': player == 0 ? 'A' : 'Z',
        'blank': false,
        'score': 1,
        'placed': false,
      },
    ],
    'scores': [0, 0],
    'remaining': [
      {'vowels': 20, 'consonants': 30},
      {'vowels': 10, 'consonants': 25},
    ],
    'board': List<dynamic>.filled(256, null),
    'bonuses': List.filled(256, ''),
    'preview': {
      'valid': false,
      'score': 0,
      'message': 'Place tiles first.',
      'words': [],
    },
    'canDraw': true,
    'canPlay': false,
    'canHint': false,
    'canShuffle': true,
  },
};

class FakeMultiplayerApi implements MultiplayerApi {
  Map<String, dynamic> view = snapshot();
  final List<String> createIds = [];
  final List<Map<String, dynamic>> requests = [];
  final List<String> joinIds = [];
  bool loseNextAction = false;
  bool loseNextJoin = false;
  bool conflictNext = false;
  Completer<Map<String, dynamic>>? heldRead;
  @override
  Future<Map<String, dynamic>> create(String requestId) async {
    createIds.add(requestId);
    return {...view, 'playerToken': hostToken};
  }

  @override
  Future<Map<String, dynamic>> join(
    String roomId,
    String token,
    String requestId,
  ) async {
    expect(token, inviteToken);
    joinIds.add(requestId);
    if (loseNextJoin) {
      loseNextJoin = false;
      throw const MultiplayerException(
        'Connection interrupted.',
        retryable: true,
      );
    }
    view = snapshot(revision: 1, joined: true, player: 1);
    view['roomId'] = roomId;
    return {...view, 'playerToken': guestToken};
  }

  @override
  Future<Map<String, dynamic>> read(String roomId, String playerToken) async =>
      heldRead == null ? view : await heldRead!.future;
  @override
  Future<Map<String, dynamic>> act(
    String roomId,
    String playerToken,
    Map<String, dynamic> request,
  ) async {
    requests.add(Map<String, dynamic>.from(request));
    if (conflictNext) {
      conflictNext = false;
      view = snapshot(revision: 3, joined: true);
      throw MultiplayerException(
        'Stale revision',
        code: 'revision_conflict',
        snapshot: view,
      );
    }
    view = snapshot(revision: 2, joined: true);
    if (loseNextAction) {
      loseNextAction = false;
      throw const MultiplayerException(
        'Connection interrupted.',
        retryable: true,
      );
    }
    return {...view, 'ok': true, 'message': 'Rack filled.'};
  }

  @override
  void close() {}
}

GameController controller(FakeMultiplayerApi api, {Uri? uri, Duration? poll}) =>
    GameController(
      multiplayerApi: api,
      initialUri: uri ?? Uri.parse('https://jot-game.pages.dev/'),
      pollInterval: poll ?? const Duration(hours: 1),
    );
