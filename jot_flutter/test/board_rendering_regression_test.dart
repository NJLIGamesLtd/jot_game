import 'package:flutter/material.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:jot_flutter/main.dart';

void main() {
  testWidgets('board updates restored tiles, ownership and current tap actions', (
    tester,
  ) async {
    final state = <String, dynamic>{
      'board': List<Map<String, dynamic>?>.filled(256, null),
      'bonuses': List<String>.filled(256, ''),
    };
    final board = state['board'] as List;
    final bonuses = state['bonuses'] as List;
    bonuses[119] = 'L2';
    var firstTaps = 0;
    var nextTaps = 0;
    Future<void> mount(void Function(int)? tap) => tester.pumpWidget(
      MaterialApp(
        home: Scaffold(
          body: SizedBox(
            width: 640,
            child: JotBoard(state: state, onTap: tap),
          ),
        ),
      ),
    );
    final square = find.byKey(const ValueKey('board-cell-119'));
    Finder text(String label) =>
        find.descendant(of: square, matching: find.text(label));
    Color? colour() => tester
        .widget<Material>(
          find.descendant(of: square, matching: find.byType(Material)),
        )
        .color;

    await mount((_) => firstTaps++);
    expect(text('L2'), findsOneWidget);
    await tester.tap(square);
    expect(firstTaps, 1);
    board[119] = {'letter': 'J', 'score': 8, 'owner': 0, 'pending': true};
    await mount((_) => nextTaps++);
    expect(text('J'), findsOneWidget);
    expect(text('8'), findsOneWidget);
    expect(text('L2'), findsNothing);
    expect(colour(), const Color(0xffeac29b));
    await tester.tap(square);
    expect(firstTaps, 1);
    expect(nextTaps, 1);

    // Same letter/score, changed ownership: the current background must update.
    board[119] = {'letter': 'J', 'score': 8, 'owner': 1, 'pending': false};
    await mount(null);
    expect(colour(), const Color(0xffc8ddd5));
    await tester.tap(square, warnIfMissed: false);
    expect(nextTaps, 1);
    board[119] = {'letter': 'A', 'score': 0, 'owner': 0, 'pending': false};
    await mount((_) => nextTaps++);
    expect(text('A'), findsOneWidget);
    expect(text('0'), findsOneWidget);
    expect(text('J'), findsNothing);
    expect(text('8'), findsNothing);
    expect(colour(), const Color(0xfffaf7ed));
    board[119] = null;
    bonuses[119] = 'W3';
    await mount((_) => nextTaps++);
    expect(text('W3'), findsOneWidget);
    expect(text('A'), findsNothing);
    expect(colour(), const Color(0xffc6cde4));
    await tester.tap(square);
    expect(nextTaps, 2);
    expect(tester.takeException(), isNull);
  });
}
