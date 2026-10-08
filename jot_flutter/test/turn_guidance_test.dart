import 'package:flutter_test/flutter_test.dart';
import 'package:jot_flutter/turn_guidance.dart';

Map<String, dynamic> snapshot({
  int rack = 8,
  int? capacity = 8,
  bool committed = true,
  int staged = 0,
  bool valid = false,
  int? score = 19,
  String? validation = 'Tiles must be aligned.',
  bool canPlay = false,
  int consonants = 12,
  int vowels = 8,
  int current = 0,
  int robot = -1,
  bool over = false,
}) => {
  'board': [
    null,
    if (committed) {'letter': 'A', 'pending': false},
    ...List.generate(staged, (_) => {'letter': 'J', 'pending': true}),
  ],
  'rack': List.generate(rack, (_) => {'letter': 'A'}),
  'rackSize': ?capacity,
  'remaining': [
    {'consonants': consonants, 'vowels': vowels},
    {'consonants': 4, 'vowels': 3},
  ],
  'preview': {'valid': valid, 'score': ?score, 'message': ?validation},
  'currentPlayer': current,
  'robotPlayer': robot,
  'over': over,
  'canPlay': canPlay,
};

void inactive(TurnGuidance guidance, String title) {
  expect(guidance.title, title);
  expect(guidance.message, isNotEmpty);
  expect(guidance.step, isNull);
  expect(guidance.needsRefill, isFalse);
  expect(guidance.returnBeforeRefill, isFalse);
}

void main() {
  group('turn guidance', () {
    test('empty rack starts drawing with the default capacity', () {
      final guidance = TurnGuidance.fromState(
        snapshot(rack: 0, capacity: null, committed: false),
      );
      expect(guidance.title, 'Draw your tiles');
      expect(
        guidance.message,
        'Draw 8 tiles to fill your rack. Choose vowels and consonants, '
        'or tap Fill rack.',
      );
      expect(guidance.step, 1);
      expect(guidance.firstMove, isTrue);
      expect(guidance.needsRefill, isTrue);
      expect(guidance.returnBeforeRefill, isFalse);
    });

    test('partly filled rack explains the remaining draw count', () {
      final guidance = TurnGuidance.fromState(snapshot(rack: 5));
      expect(guidance.title, 'Fill your rack');
      expect(guidance.message, contains('Draw 3 more tiles to reach 8'));
      expect(guidance.step, 1);
      expect(guidance.firstMove, isFalse);
      expect(guidance.needsRefill, isTrue);
      expect(guidance.returnBeforeRefill, isFalse);
    });

    test('valid JAR with five total tiles must return then draw three', () {
      final guidance = TurnGuidance.fromState(
        snapshot(rack: 5, staged: 2, valid: true),
      );
      expect(guidance.title, 'Fill your rack first');
      expect(
        guidance.message,
        'Your word is valid for 19 points. Return the placed tiles, '
        'then draw 3 more tiles to reach 8. '
        'Your placed tiles still count toward your rack.',
      );
      expect(guidance.step, 1);
      expect(guidance.needsRefill, isTrue);
      expect(guidance.returnBeforeRefill, isTrue);
    });

    test('a short rack wins over an inconsistent play permission', () {
      final guidance = TurnGuidance.fromState(
        snapshot(rack: 5, staged: 2, valid: true, canPlay: true),
      );
      expect(guidance.step, 1);
      expect(guidance.needsRefill, isTrue);
      expect(guidance.title, isNot('Play your word'));
    });

    test('one missing tile and one staged tile use singular wording', () {
      final guidance = TurnGuidance.fromState(
        snapshot(rack: 7, staged: 1, valid: true, score: 1),
      );
      expect(guidance.message, contains('valid for 1 point.'));
      expect(
        guidance.message,
        contains('Return the placed tile, then draw 1 more tile'),
      );
      expect(guidance.returnBeforeRefill, isTrue);
    });

    test('an invalid staged short rack still requires refill first', () {
      final guidance = TurnGuidance.fromState(snapshot(rack: 6, staged: 1));
      expect(guidance.message, startsWith('Return the placed tile'));
      expect(guidance.message, contains('draw 2 more tiles'));
      expect(guidance.message, isNot(contains('word is valid')));
      expect(guidance.needsRefill, isTrue);
    });

    test('custom capacity changes both the count and destination', () {
      final guidance = TurnGuidance.fromState(snapshot(rack: 3, capacity: 4));
      expect(guidance.message, contains('Draw 1 more tile to reach 4'));
      final empty = TurnGuidance.fromState(snapshot(rack: 0, capacity: 1));
      expect(empty.message, startsWith('Draw 1 tile'));
    });

    test('full first rack explains one of the four centre squares', () {
      final guidance = TurnGuidance.fromState(snapshot(committed: false));
      expect(guidance.title, 'Place your word');
      expect(guidance.message, contains('across or down'));
      expect(
        guidance.message,
        contains('at least one of the four outlined centre squares'),
      );
      expect(guidance.firstMove, isTrue);
      expect(guidance.step, 2);
      expect(guidance.needsRefill, isFalse);
    });

    test('staged first move does not count as a committed board', () {
      final guidance = TurnGuidance.fromState(
        snapshot(committed: false, staged: 2),
      );
      expect(guidance.firstMove, isTrue);
      expect(guidance.title, 'Adjust your word');
    });

    test('existing board explains connecting to an existing word', () {
      final guidance = TurnGuidance.fromState(snapshot());
      expect(guidance.title, 'Place your word');
      expect(
        guidance.message,
        contains('connecting to a word already on the board'),
      );
      expect(guidance.firstMove, isFalse);
      expect(guidance.step, 2);
      expect(guidance.returnBeforeRefill, isFalse);
    });

    test('empty bags allow placement with a short rack', () {
      for (final committed in [false, true]) {
        final guidance = TurnGuidance.fromState(
          snapshot(rack: 3, consonants: 0, vowels: 0, committed: committed),
        );
        expect(guidance.title, 'Place your word');
        expect(guidance.message, startsWith('Your bags are empty'));
        expect(guidance.needsRefill, isFalse);
        expect(guidance.step, 2);
      }
    });

    test('one remaining bag still requires drawing', () {
      for (final bag in ['consonants', 'vowels']) {
        final guidance = TurnGuidance.fromState(
          snapshot(
            rack: 7,
            consonants: bag == 'consonants' ? 1 : 0,
            vowels: bag == 'vowels' ? 1 : 0,
          ),
        );
        expect(guidance.needsRefill, isTrue);
      }
    });

    test('engine permission advances a full valid word to playing', () {
      final guidance = TurnGuidance.fromState(
        snapshot(staged: 2, valid: true, canPlay: true),
      );
      expect(guidance.title, 'Play your word');
      expect(
        guidance.message,
        'This word is worth 19 points. Tap Play word to finish your turn.',
      );
      expect(guidance.step, 3);
      expect(guidance.needsRefill, isFalse);
    });

    test('short exhausted rack can play when the engine permits it', () {
      final guidance = TurnGuidance.fromState(
        snapshot(
          rack: 3,
          staged: 2,
          valid: true,
          canPlay: true,
          consonants: 0,
          vowels: 0,
        ),
      );
      expect(guidance.title, 'Play your word');
      expect(guidance.step, 3);
      expect(guidance.needsRefill, isFalse);
      expect(guidance.returnBeforeRefill, isFalse);
    });

    test('one point uses singular score wording', () {
      final guidance = TurnGuidance.fromState(
        snapshot(staged: 1, valid: true, canPlay: true, score: 1),
      );
      expect(guidance.message, contains('worth 1 point.'));
    });

    test('preview validity alone never claims the word can be played', () {
      for (final permission in [false, null]) {
        final state = snapshot(staged: 2, valid: true);
        state['canPlay'] = permission;
        final guidance = TurnGuidance.fromState(state);
        expect(guidance.title, 'Checking your word');
        expect(guidance.message, contains('Wait until Play word is available'));
        expect(guidance.step, 2);
      }
    });

    test('invalid placement preserves the engine explanation', () {
      final guidance = TurnGuidance.fromState(
        snapshot(staged: 2, validation: 'Not all formed words are valid.'),
      );
      expect(guidance.title, 'Adjust your word');
      expect(guidance.message, startsWith('Not all formed words are valid.'));
      expect(guidance.message, contains('Move or return your placed tiles'));
      expect(guidance.step, 2);
    });

    test('missing or blank validation messages receive a useful fallback', () {
      for (final validation in [null, '', '   ']) {
        final guidance = TurnGuidance.fromState(
          snapshot(staged: 2, validation: validation),
        );
        expect(
          guidance.message,
          startsWith('Check your word and its placement.'),
        );
      }
    });

    test('missing optional snapshot data does not invent empty bags', () {
      final guidance = TurnGuidance.fromState({});
      expect(guidance.title, 'Draw your tiles');
      expect(guidance.needsRefill, isTrue);
      expect(guidance.firstMove, isTrue);
      final state = snapshot(rack: 2, score: null)..remove('remaining');
      expect(TurnGuidance.fromState(state).needsRefill, isTrue);
    });

    test('unknown bag fields do not imply exhaustion', () {
      final state = snapshot(rack: 7);
      state['remaining'] = [null];
      expect(TurnGuidance.fromState(state).needsRefill, isTrue);
      state['remaining'] = [{}];
      expect(TurnGuidance.fromState(state).needsRefill, isTrue);
      state['currentPlayer'] = -1;
      state['robotPlayer'] = 1;
      expect(TurnGuidance.fromState(state).needsRefill, isTrue);
    });

    test('the current player’s bags determine refill requirements', () {
      final state = snapshot(rack: 3, consonants: 0, vowels: 0, current: 1);
      expect(TurnGuidance.fromState(state).needsRefill, isTrue);
    });

    test(
      'missing preview and score fields receive safe presentation defaults',
      () {
        final state = snapshot(staged: 1)..remove('preview');
        expect(TurnGuidance.fromState(state).title, 'Adjust your word');
        state['canPlay'] = true;
        expect(TurnGuidance.fromState(state).message, contains('0 points'));
      },
    );

    test('game over overrides connection, failure, and busy states', () {
      inactive(
        TurnGuidance.fromState(
          snapshot(over: true),
          isOnline: true,
          connected: false,
          failed: true,
          busy: true,
        ),
        'Game over',
      );
    });

    test('failed loading explains retry without suggesting turn actions', () {
      inactive(
        TurnGuidance.fromState(snapshot(rack: 0), failed: true, joining: true),
        'Let’s get you back in',
      );
    });

    test('joining takes priority over disconnected and waiting states', () {
      inactive(
        TurnGuidance.fromState(
          snapshot(),
          joining: true,
          isOnline: true,
          connected: false,
          joined: false,
        ),
        'Joining your game',
      );
    });

    test('offline online game explains reconnecting first', () {
      inactive(
        TurnGuidance.fromState(
          snapshot(rack: 0),
          isOnline: true,
          connected: false,
          joined: false,
          busy: true,
        ),
        'Reconnecting',
      );
    });

    test('a connected unjoined room explains the invitation', () {
      inactive(
        TurnGuidance.fromState(
          snapshot(),
          isOnline: true,
          joined: false,
          busy: true,
        ),
        'Invite your friend',
      );
    });

    test('another player’s turn does not encourage refill or playing', () {
      inactive(
        TurnGuidance.fromState(
          snapshot(rack: 0, current: 1),
          isOnline: true,
          yourPlayer: 0,
          busy: true,
        ),
        'Your friend’s turn',
      );
      inactive(
        TurnGuidance.fromState(snapshot(), isOnline: true),
        'Your friend’s turn',
      );
    });

    test('the computer’s turn explains waiting even during its operation', () {
      inactive(
        TurnGuidance.fromState(snapshot(current: 1, robot: 1), busy: true),
        'Computer’s turn',
      );
    });

    test('busy human turn waits without proposing an action', () {
      inactive(
        TurnGuidance.fromState(snapshot(rack: 0), busy: true),
        'One moment',
      );
    });

    test('local connection flags do not interrupt a human local turn', () {
      expect(
        TurnGuidance.fromState(
          snapshot(),
          connected: false,
          joined: false,
        ).step,
        2,
      );
    });

    test('online caller’s active turn receives the normal draw step', () {
      final guidance = TurnGuidance.fromState(
        snapshot(rack: 0, robot: 0),
        isOnline: true,
        yourPlayer: 0,
      );
      expect(guidance.step, 1);
      expect(guidance.title, 'Draw your tiles');
    });
  });
}
