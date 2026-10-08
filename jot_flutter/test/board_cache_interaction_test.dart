import 'dart:ui' show SemanticsAction;

import 'package:flutter/material.dart';
import 'package:flutter/rendering.dart' show RenderParagraph;
import 'package:flutter/services.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:jot_flutter/main.dart';

Map<String, dynamic> emptyBoard() => {
  'board': List<Map<String, dynamic>?>.filled(256, null),
  'bonuses': List<String>.filled(256, ''),
};

const squareKey = ValueKey('board-cell-119');
const payload = RackTileDrag(
  rackIndex: 0,
  letter: 'A',
  blank: false,
  revision: 1,
  roomId: null,
  onlineRevision: -1,
  player: 0,
);

Future<void> mountBoard(
  WidgetTester tester,
  Map<String, dynamic> state, {
  void Function(int)? onTap,
  bool Function(RackTileDrag)? canDrop,
  void Function(RackTileDrag, int)? onDrop,
  RackTileDrag Function(int, String)? dragForRack,
  bool canDrag = false,
  double textScale = 1,
}) => tester.pumpWidget(
  MaterialApp(
    home: MediaQuery(
      data: MediaQueryData(textScaler: TextScaler.linear(textScale)),
      child: Scaffold(
        body: SizedBox(
          width: 640,
          child: JotBoard(
            state: state,
            onTap: onTap,
            canDrop: canDrop,
            onDrop: onDrop,
            dragForRack: dragForRack,
            canDrag: canDrag,
          ),
        ),
      ),
    ),
  ),
);

void main() {
  testWidgets(
    'Unchanged cached squares use current pointer and semantics actions',
    (tester) async {
      final state = emptyBoard();
      final square = find.byKey(squareKey);
      final semantics = tester.ensureSemantics();
      var firstTaps = 0;
      var currentTaps = 0;
      try {
        await mountBoard(tester, state, onTap: (_) => firstTaps++);
        await tester.tap(square);
        expect(firstTaps, 1);
        await mountBoard(tester, state, onTap: (_) => currentTaps++);
        await tester.tap(square);
        var node = tester.getSemantics(square);
        expect(node.getSemanticsData().hasAction(SemanticsAction.tap), isTrue);
        node.owner!.performAction(node.id, SemanticsAction.tap);
        expect(firstTaps, 1);
        expect(currentTaps, 2);

      await mountBoard(tester, state);
      await tester.tap(square, warnIfMissed: false);
        node = tester.getSemantics(square);
        expect(node.getSemanticsData().hasAction(SemanticsAction.tap), isFalse);
        expect(currentTaps, 2);

        await mountBoard(tester, state, onTap: (_) => currentTaps++);
        await tester.tap(square);
        expect(currentTaps, 3);
        expect(tester.takeException(), isNull);
      } finally {
        semantics.dispose();
      }
    },
  );

  testWidgets('Cached keyboard focus cannot activate a disabled square', (
    tester,
  ) async {
    final state = emptyBoard();
    final square = find.byKey(squareKey);
    var firstTaps = 0;
    var currentTaps = 0;
    await mountBoard(tester, state, onTap: (_) => firstTaps++);
    final region = find
        .descendant(of: square, matching: find.byType(MouseRegion))
        .first;
    final focus = Focus.of(tester.element(region));
    focus.requestFocus();
    await tester.pump();
    expect(focus.hasFocus, isTrue);
    await tester.sendKeyEvent(LogicalKeyboardKey.space);
    expect(firstTaps, 1);

    await mountBoard(tester, state);
    await tester.pump();
    expect(focus.canRequestFocus, isFalse);
    expect(focus.hasFocus, isFalse);
    await tester.sendKeyEvent(LogicalKeyboardKey.space);
    expect(firstTaps, 1);

    await mountBoard(tester, state, onTap: (_) => currentTaps++);
    focus.requestFocus();
    await tester.pump();
    await tester.sendKeyEvent(LogicalKeyboardKey.space);
    expect(firstTaps, 1);
    expect(currentTaps, 1);
    expect(tester.takeException(), isNull);
  });

  testWidgets('Cached empty drag targets read current eligibility and delivery', (
    tester,
  ) async {
    final state = emptyBoard();
    final square = find.byKey(squareKey);
    var staleDrops = 0;
    final currentDrops = <int>[];
    DragTarget<RackTileDrag> target() =>
        tester.widget<DragTarget<RackTileDrag>>(
          find.descendant(
            of: square,
            matching: find.byType(DragTarget<RackTileDrag>),
          ),
        );
    final details = DragTargetDetails(data: payload, offset: Offset.zero);

    await mountBoard(
      tester,
      state,
      onTap: (_) {},
      canDrop: (_) => true,
      onDrop: (_, _) => staleDrops++,
    );
    expect(target().onWillAcceptWithDetails!(details), isTrue);
    await mountBoard(
      tester,
      state,
      onTap: (_) {},
      canDrop: (_) => false,
      onDrop: (_, cell) => currentDrops.add(cell),
    );
    expect(target().onWillAcceptWithDetails!(details), isFalse);
    await mountBoard(
      tester,
      state,
      onTap: (_) {},
      canDrop: (_) => true,
      onDrop: (_, cell) => currentDrops.add(cell),
    );
    expect(target().onWillAcceptWithDetails!(details), isTrue);
    target().onAcceptWithDetails!(details);
    expect(staleDrops, 0);
    expect(currentDrops, [119]);
    // Occupancy is also read live, rather than captured when a target was built.
    (state['board'] as List)[119] = {'letter': 'B', 'score': 3, 'owner': 0};
    expect(target().onWillAcceptWithDetails!(details), isFalse);
  });

  testWidgets(
    'Unchanged staged faces refresh payload and disable future drags',
    (tester) async {
      final state = emptyBoard();
      (state['board'] as List)[119] = {
        'letter': 'A',
        'score': 1,
        'owner': 0,
        'pending': true,
        'rack': 0,
      };
      final square = find.byKey(squareKey);
      Draggable<RackTileDrag> draggable() =>
          tester.widget<Draggable<RackTileDrag>>(
            find.descendant(
              of: square,
              matching: find.byType(Draggable<RackTileDrag>),
            ),
          );
      await mountBoard(
        tester,
        state,
        onTap: (_) {},
        canDrag: true,
        dragForRack: (_, _) => payload,
      );
      expect(draggable().data, payload);
      expect(draggable().maxSimultaneousDrags, 1);
      const fresh = RackTileDrag(
        rackIndex: 0,
        letter: 'A',
        blank: true,
        revision: 2,
        roomId: 'new-room',
        onlineRevision: 8,
        player: 1,
      );
      await mountBoard(tester, state, dragForRack: (_, _) => fresh);
      expect(draggable().data, fresh);
      expect(draggable().maxSimultaneousDrags, 0);
      await mountBoard(
        tester,
        state,
        onTap: (_) {},
        canDrag: true,
        dragForRack: (_, _) => fresh,
      );
      expect(draggable().data, fresh);
      expect(draggable().maxSimultaneousDrags, 1);
    },
  );

  testWidgets(
    'Cached tile text continues to respond to inherited text scaling',
    (tester) async {
      final state = emptyBoard();
      (state['board'] as List)[119] = {'letter': 'J', 'score': 8, 'owner': 0};
      final label = find.descendant(
        of: find.byKey(squareKey),
        matching: find.text('J'),
      );
      await mountBoard(tester, state);
      expect(
        tester.renderObject<RenderParagraph>(label).textScaler.scale(19),
        19,
      );
      await mountBoard(tester, state, textScale: 2);
      expect(
        tester.renderObject<RenderParagraph>(label).textScaler.scale(19),
        38,
      );
      expect(tester.takeException(), isNull);
    },
  );
}
