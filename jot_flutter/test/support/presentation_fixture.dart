import 'package:jot_flutter/game_controller.dart';

class PresentationFixture extends GameController {
  String? operation;
  Map<String, dynamic>? fields;
  PresentationFixture() {
    busy = false;
    message = 'Choose a tile.';
    state = {
      'currentPlayer': 0,
      'robotPlayer': 1,
      'over': false,
      'canPlay': false,
      'canDraw': true,
      'canHint': false,
      'canShuffle': true,
      'scores': [12, 8],
      'board': List<dynamic>.filled(256, null),
      'bonuses': List<String>.filled(256, ''),
      'rack': [
        {
          'letter': 'A',
          'blank': false,
          'owner': 0,
          'placed': false,
          'score': 1,
        },
      ],
      'remaining': [
        {'vowels': 26, 'consonants': 37},
        {'vowels': 27, 'consonants': 37},
      ],
      'preview': {
        'valid': false,
        'score': 0,
        'message': 'Place tiles first.',
        'words': [],
      },
    };
  }
  @override
  Future<bool> act(String op, [Map<String, dynamic> data = const {}]) async {
    operation = op;
    fields = data;
    return true;
  }
}

class WaitingOnlineFixture extends PresentationFixture {
  WaitingOnlineFixture() {
    connected = true;
    message = 'Waiting for your friend’s turn.';
    state!['robotPlayer'] = -1;
    state!['remaining'] = [
      {'vowels': 26, 'consonants': 37},
      {'vowels': 10, 'consonants': 25},
    ];
    state!['rack'] = [
      {'letter': 'Z', 'blank': false, 'owner': 1, 'placed': false, 'score': 10},
    ];
  }
  @override
  bool get isOnline => true;
  @override
  int? get yourPlayer => 1;
  @override
  bool get joined => true;
}
