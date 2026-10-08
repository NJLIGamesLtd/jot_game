/// Explains the next turn action without replacing the engine's validation.
class TurnGuidance {
  final String title;
  final String message;
  final int? step;
  final bool needsRefill;
  final bool returnBeforeRefill;
  final bool firstMove;

  const TurnGuidance._({
    required this.title,
    required this.message,
    required this.step,
    required this.needsRefill,
    required this.returnBeforeRefill,
    required this.firstMove,
  });

  factory TurnGuidance.fromState(
    Map<String, dynamic> state, {
    bool isOnline = false,
    bool joined = true,
    bool connected = true,
    bool joining = false,
    bool busy = false,
    bool failed = false,
    int? yourPlayer,
  }) {
    final board = state['board'] as List? ?? const [];
    final staged = board.whereType<Map>().where(
      (tile) => tile['pending'] == true,
    );
    final firstMove = !board.whereType<Map>().any(
      (tile) => tile['pending'] != true,
    );
    TurnGuidance tip(
      String title,
      String message, {
      int? step,
      bool needsRefill = false,
      bool returnBeforeRefill = false,
    }) => TurnGuidance._(
      title: title,
      message: message,
      step: step,
      needsRefill: needsRefill,
      returnBeforeRefill: returnBeforeRefill,
      firstMove: firstMove,
    );

    if (state['over'] == true) {
      return tip(
        'Game over',
        'Review the board or start a new game to play again.',
      );
    }
    if (failed) {
      return tip('Let’s get you back in', 'Use Retry to reload your game.');
    }
    if (joining) {
      return tip('Joining your game', 'Please wait while we open your game.');
    }
    if (isOnline && !connected) {
      return tip(
        'Reconnecting',
        'Reconnect before drawing, placing, or playing.',
      );
    }
    if (isOnline && !joined) {
      return tip(
        'Invite your friend',
        'Share the invitation link. The game starts when your friend joins.',
      );
    }
    final current = state['currentPlayer'] as int? ?? 0;
    if (isOnline && yourPlayer != current) {
      return tip(
        'Your friend’s turn',
        'Wait for your friend to finish their turn.',
      );
    }
    if (!isOnline && state['robotPlayer'] == current) {
      return tip(
        'Computer’s turn',
        'The computer is choosing its move. Your turn comes next.',
      );
    }
    if (busy) {
      return tip('One moment', 'Please wait while your game updates.');
    }

    // Staged tiles remain in the engine's rack array until the move is played.
    final rack = state['rack'] as List? ?? const [];
    final capacity = state['rackSize'] as int? ?? 8;
    final remaining = state['remaining'] as List? ?? const [];
    final bags = current >= 0 && current < remaining.length
        ? remaining[current] as Map?
        : null;
    final depleted =
        bags != null && bags['consonants'] == 0 && bags['vowels'] == 0;
    final preview = state['preview'] as Map? ?? const {};
    final valid = preview['valid'] == true;
    final score = (preview['score'] as num?)?.toInt() ?? 0;
    final points = '$score ${score == 1 ? 'point' : 'points'}';
    final missing = capacity - rack.length;
    if (missing > 0 && !depleted) {
      final tiles = missing == 1 ? 'tile' : 'tiles';
      if (staged.isNotEmpty) {
        final placed = staged.length == 1 ? 'tile' : 'tiles';
        final prefix = valid ? 'Your word is valid for $points. ' : '';
        return tip(
          'Fill your rack first',
          '${prefix}Return the placed $placed, then draw $missing more $tiles '
              'to reach $capacity. Your placed tiles still count toward your rack.',
          step: 1,
          needsRefill: true,
          returnBeforeRefill: true,
        );
      }
      return tip(
        rack.isEmpty ? 'Draw your tiles' : 'Fill your rack',
        rack.isEmpty
            ? 'Draw $missing $tiles to fill your rack. Choose vowels and '
                  'consonants, or tap Fill rack.'
            : 'Draw $missing more $tiles to reach $capacity before placing a word. '
                  'Choose vowels and consonants, or tap Fill rack.',
        step: 1,
        needsRefill: true,
      );
    }

    // A valid preview alone is insufficient: only the engine permits playing.
    if (state['canPlay'] == true) {
      return tip(
        'Play your word',
        'This word is worth $points. Tap Play word to finish your turn.',
        step: 3,
      );
    }
    if (staged.isNotEmpty) {
      if (valid) {
        return tip(
          'Checking your word',
          'Your word looks valid. Wait until Play word is available.',
          step: 2,
        );
      }
      final validation = preview['message'] as String? ?? '';
      final explanation = validation.trim().isEmpty
          ? 'Check your word and its placement.'
          : validation;
      return tip(
        'Adjust your word',
        '$explanation Move or return your placed tiles, then try again.',
        step: 2,
      );
    }
    final prefix = depleted
        ? 'Your bags are empty, so use the tiles you have. '
        : '';
    return tip(
      'Place your word',
      firstMove
          ? '${prefix}Place a word across or down. Cover at least one of the '
                'four outlined centre squares.'
          : '${prefix}Place a word across or down, connecting to a word already '
                'on the board.',
      step: 2,
    );
  }
}
