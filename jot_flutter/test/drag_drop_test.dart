import 'dart:async';
import 'dart:ui' show PointerDeviceKind;

import 'package:flutter/material.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:jot_core/jot_core.dart';
import 'package:jot_flutter/game_controller.dart';
import 'package:jot_flutter/main.dart';
import 'package:shared_preferences/shared_preferences.dart';

import 'support/drag_fixture.dart';

void main() {
  setUp(() => SharedPreferences.setMockInitialValues({}));

  for (final size in [const Size(390, 844), const Size(1440, 1000)]) {
    testWidgets(
      'Touch drag shows the tile under the finger and releases into its exact cell at ${size.width}px',
      (tester) async {
        final game = DragFixture();
        await mountDrag(tester, game, size: size);
        final sourceSize = tester.getSize(rack(0));
        final sourcePosition = tester.getCenter(rack(0));
        expect(sourcePosition.dx, inInclusiveRange(0, size.width));
        expect(sourcePosition.dy, inInclusiveRange(0, size.height));
        final pointer = await beginDrag(tester, rack(0));
        expect(
          feedback,
          findsOneWidget,
          reason: 'The tile must be visible during immediate touch movement.',
        );
        expect(tester.getSize(feedback), sourceSize);
        final destination = tester.getCenter(cell(119));
        expect(destination.dx, inInclusiveRange(0, size.width));
        expect(destination.dy, inInclusiveRange(0, size.height));
        await pointer.moveTo(destination);
        await tester.pump();
        expect(
          (tester.getCenter(feedback) - destination).distance,
          lessThan(1),
        );
        expect(
          find.byKey(const ValueKey('drop-highlight-119')),
          findsOneWidget,
          reason:
              'Destination $destination; board ${tester.getRect(find.byType(JotBoard))}; visible highlights ${find.byWidgetPredicate((widget) => widget.key.toString().contains("drop-highlight")).evaluate().map((element) => element.widget.key)}',
        );
        expect(
          find.descendant(of: feedback, matching: find.text('A')),
          findsOneWidget,
        );
        await pointer.up();
        await tester.pump();
        expect(feedback, findsNothing);
        expect(game.commands.single, {
          'op': 'place',
          'rack': 0,
          'row': 7,
          'col': 7,
          'letter': 'A',
        });
        expect(tester.takeException(), isNull);
      },
    );
  }

  testWidgets('Desktop mouse dragging uses the same immediate placement path', (
    tester,
  ) async {
    final game = DragFixture();
    await mountDrag(tester, game);
    final pointer = await beginDrag(
      tester,
      rack(0),
      kind: PointerDeviceKind.mouse,
    );
    expect(feedback, findsOneWidget);
    await pointer.moveTo(tester.getCenter(cell(119)));
    await tester.pump();
    await pointer.up();
    await tester.pump();
    expect(game.commands.single, {
      'op': 'place',
      'rack': 0,
      'row': 7,
      'col': 7,
      'letter': 'A',
    });
  });

  testWidgets(
    'Empty board taps require a selected rack tile and occupied taps preserve it',
    (tester) async {
      final game = DragFixture();
      (game.state!['board'] as List)[120] = {
        'letter': 'C',
        'pending': false,
        'owner': 0,
        'score': 3,
      };
      await mountDrag(tester, game);
      await tester.tap(cell(119));
      await tester.pump();
      expect(game.commands, isEmpty);
      await tester.tap(rack(0));
      await tester.pump();
      await tester.tap(cell(120));
      await tester.pump();
      expect(game.commands, isEmpty);
      expect(tester.widget<RackTileFace>(rack(0)).selected, isTrue);
      expect((game.state!['board'] as List)[120]['letter'], 'C');
    },
  );

  testWidgets('A shortened rack clears an out-of-range selection on rebuild', (
    tester,
  ) async {
    final game = DragFixture();
    await mountDrag(tester, game);
    await tester.tap(rack(2));
    await tester.pump();
    expect(tester.widget<RackTileFace>(rack(2)).selected, isTrue);
    (game.state!['rack'] as List).removeLast();
    game.changed();
    await tester.pump();
    expect(rack(2), findsNothing);
    expect(tester.widget<RackTileFace>(rack(0)).selected, isFalse);
    expect(tester.widget<RackTileFace>(rack(1)).selected, isFalse);
    await tester.tap(cell(119));
    await tester.pump();
    expect(game.commands, isEmpty);
    expect(tester.takeException(), isNull);
  });

  testWidgets('An unplace response arriving after disposal does not rebuild', (
    tester,
  ) async {
    final game = DragFixture();
    (game.state!['board'] as List)[119] = {
      'letter': 'A',
      'pending': true,
      'rack': 0,
      'owner': 0,
      'score': 1,
    };
    final held = Completer<void>();
    game.action = (_) => held.future;
    await mountDrag(tester, game);
    await tester.tap(cell(119));
    await tester.pump();
    expect(game.commands.single, {'op': 'unplace', 'rack': 0});
    await tester.pumpWidget(const SizedBox());
    held.complete();
    await tester.pump();
    expect(tester.takeException(), isNull);
  });

  testWidgets('An unplace response shrinking the rack clears its selection', (
    tester,
  ) async {
    final game = DragFixture();
    (game.state!['board'] as List)[119] = {
      'letter': 'A',
      'pending': true,
      'rack': 0,
      'owner': 0,
      'score': 1,
    };
    game.action = (_) async {
      (game.state!['rack'] as List).clear();
      (game.state!['board'] as List)[119] = null;
      game.state!['revision'] = 8;
    };
    await mountDrag(tester, game);
    await tester.tap(rack(0));
    await tester.pump();
    expect(tester.widget<RackTileFace>(rack(0)).selected, isTrue);
    await tester.tap(cell(119));
    await tester.pump();
    expect(game.commands.single, {'op': 'unplace', 'rack': 0});
    expect(rack(0), findsNothing);
    await tester.tap(cell(120));
    await tester.pump();
    expect(game.commands, hasLength(1));
    expect(tester.takeException(), isNull);
  });

  testWidgets(
    'Invalid drag identities and occupied acceptance callbacks do not bypass placement validation',
    (tester) async {
      final game = DragFixture();
      await mountDrag(tester, game);
      final dynamic page = tester.state(find.byType(GamePage));
      final original = page.dragForRack(0) as RackTileDrag;
      RackTileDrag altered({
        int? rackIndex,
        int? player,
        String? letter,
        bool? blank,
      }) => RackTileDrag(
        rackIndex: rackIndex ?? original.rackIndex,
        letter: letter ?? original.letter,
        blank: blank ?? original.blank,
        revision: original.revision,
        roomId: original.roomId,
        onlineRevision: original.onlineRevision,
        player: player ?? original.player,
      );
      for (final payload in [
        altered(rackIndex: -1),
        altered(rackIndex: 100),
        altered(player: 1),
        altered(letter: 'Z'),
        altered(blank: true),
      ]) {
        expect(page.canDrop(payload), isFalse);
        await page.dropAt(payload, 119);
      }
      (game.state!['board'] as List)[119] = {
        'letter': 'C',
        'owner': 0,
        'pending': false,
        'score': 3,
      };
      await page.dropAt(original, 119);
      expect(game.commands, isEmpty);
    },
  );

  testWidgets('Release outside the board cancels without changing the rack', (
    tester,
  ) async {
    final game = DragFixture();
    await mountDrag(tester, game);
    await dragTo(tester, rack(0), const Offset(10, 10));
    expect(game.commands, isEmpty);
    expect((game.state!['rack'] as List)[0]['letter'], 'A');
    expect(feedback, findsNothing);
  });

  testWidgets(
    'An occupied board square rejects the dragged tile and has no drop highlight',
    (tester) async {
      final game = DragFixture();
      (game.state!['board'] as List)[119] = {
        'letter': 'C',
        'pending': false,
        'owner': 0,
        'score': 3,
      };
      await mountDrag(tester, game);
      final pointer = await beginDrag(tester, rack(0));
      await pointer.moveTo(tester.getCenter(cell(119)));
      await tester.pump();
      expect(find.byKey(const ValueKey('drop-highlight-119')), findsNothing);
      await pointer.up();
      await tester.pump();
      expect(game.commands, isEmpty);
      expect((game.state!['board'] as List)[119]['letter'], 'C');
    },
  );

  testWidgets(
    'Dropping a blank asks for a letter and stages the chosen letter',
    (tester) async {
      final game = DragFixture();
      await mountDrag(tester, game);
      await dragTo(tester, rack(2), tester.getCenter(cell(119)));
      await tester.pumpAndSettle();
      expect(find.text('Choose a letter for your blank'), findsOneWidget);
      await tester.tap(find.widgetWithText(OutlinedButton, 'Z'));
      await tester.pumpAndSettle();
      expect(game.commands.single, {
        'op': 'place',
        'rack': 2,
        'row': 7,
        'col': 7,
        'letter': 'Z',
      });
    },
  );

  testWidgets('Cancelling a blank choice preserves the unassigned rack tile', (
    tester,
  ) async {
    final game = DragFixture();
    await mountDrag(tester, game);
    await dragTo(tester, rack(2), tester.getCenter(cell(119)));
    await tester.pumpAndSettle();
    await tester.tapAt(const Offset(10, 10));
    await tester.pumpAndSettle();
    expect(game.commands, isEmpty);
    expect((game.state!['rack'] as List)[2]['letter'], '@');
  });

  for (final blank in [false, true]) {
    testWidgets(
      'A staged ${blank ? 'assigned blank' : 'tile'} moves by its original rack index',
      (tester) async {
        final game = DragFixture();
        final index = blank ? 2 : 0;
        final letter = blank ? 'Q' : 'A';
        ((game.state!['rack'] as List)[index] as Map)['placed'] = true;
        (game.state!['board'] as List)[119] = {
          'letter': letter,
          'blank': blank,
          'pending': true,
          'rack': index,
          'owner': 0,
          'score': blank ? 0 : 1,
        };
        await mountDrag(tester, game);
        final pointer = await beginDrag(tester, cell(119));
        expect(
          find.descendant(of: feedback, matching: find.text(letter)),
          findsOneWidget,
        );
        await pointer.moveTo(tester.getCenter(cell(120)));
        await tester.pump();
        await pointer.up();
        await tester.pumpAndSettle();
        expect(find.text('Choose a letter for your blank'), findsNothing);
        expect(game.commands.single, {
          'op': 'place',
          'rack': index,
          'row': 7,
          'col': 8,
          'letter': letter,
        });
      },
    );
  }

  for (final disabled in ['waiting', 'busy', 'over']) {
    testWidgets('Dragging is disabled while $disabled', (tester) async {
      final game = DragFixture();
      if (disabled == 'waiting') {
        game.online = true;
        game.connected = true;
        game.state!['currentPlayer'] = 1;
      }
      if (disabled == 'busy') game.busy = true;
      if (disabled == 'over') game.state!['over'] = true;
      await mountDrag(tester, game);
      await dragTo(tester, rack(0), tester.getCenter(cell(119)));
      expect(feedback, findsNothing);
      expect(game.commands, isEmpty);
    });
  }

  for (final change in [
    'revision',
    'online revision',
    'room',
    'player',
    'rack letter',
    'rack blank',
    'rack removed',
  ]) {
    testWidgets('An in-flight drag is rejected after the $change changes', (
      tester,
    ) async {
      final game = DragFixture()
        ..online = true
        ..connected = true;
      game.state!['robotPlayer'] = -1;
      await mountDrag(tester, game);
      final pointer = await beginDrag(tester, rack(0));
      await pointer.moveTo(tester.getCenter(cell(119)));
      await tester.pump();
      switch (change) {
        case 'revision':
          game.state!['revision'] = 8;
        case 'online revision':
          game.networkRevision++;
        case 'room':
          game.activeRoom = 'abcdef0123456789abcdef0123456789';
        case 'player':
          game.state!['currentPlayer'] = 1;
        case 'rack letter':
          ((game.state!['rack'] as List)[0] as Map)['letter'] = 'Z';
        case 'rack blank':
          ((game.state!['rack'] as List)[0] as Map)['blank'] = true;
        case 'rack removed':
          (game.state!['rack'] as List).clear();
      }
      game.changed();
      await tester.pump();
      await pointer.up();
      await tester.pump();
      expect(game.commands, isEmpty);
    });
  }

  testWidgets(
    'A blank chosen after another device advances the room cannot use a stale drag',
    (tester) async {
      final game = DragFixture()
        ..online = true
        ..connected = true;
      game.state!['robotPlayer'] = -1;
      await mountDrag(tester, game);
      await dragTo(tester, rack(2), tester.getCenter(cell(119)));
      await tester.pumpAndSettle();
      game.networkRevision++;
      game.changed();
      await tester.pump();
      await tester.tap(find.widgetWithText(OutlinedButton, 'Z'));
      await tester.pumpAndSettle();
      expect(game.commands, isEmpty);
    },
  );

  testWidgets(
    'Pinch-zoomed board targets retain exact row and column during a rack drag',
    (tester) async {
      final game = DragFixture();
      await mountDrag(tester, game);
      final centre = tester.getCenter(find.byType(JotBoard));
      final before = tester.getCenter(cell(120));
      final first = await tester.startGesture(
        centre - const Offset(40, 0),
        pointer: 1,
      );
      final second = await tester.startGesture(
        centre + const Offset(40, 0),
        pointer: 2,
      );
      await first.moveTo(centre - const Offset(100, 0));
      await second.moveTo(centre + const Offset(100, 0));
      await tester.pump();
      await first.up();
      await second.up();
      await tester.pump();
      final after = tester.getCenter(cell(120));
      expect(
        (after - before).distance,
        greaterThan(3),
        reason: 'The board must really be transformed before the drop.',
      );
      await dragTo(tester, rack(0), after);
      expect(game.commands.single, {
        'op': 'place',
        'rack': 0,
        'row': 7,
        'col': 8,
        'letter': 'A',
      });
    },
  );

  testWidgets(
    'Touch rack and staged-board drags use the actual native C++ placement and saved state',
    (tester) async {
      tester.view.physicalSize = const Size(1440, 1000);
      tester.view.devicePixelRatio = 1;
      addTearDown(tester.view.resetPhysicalSize);
      addTearDown(tester.view.resetDevicePixelRatio);
      final game = GameController(
        coreFactory: (dictionary) => createCore(dictionary, seed: 12345),
      );
      await tester.runAsync(game.initialize);
      await tester.runAsync(() => game.act('fill'));
      await tester.pumpWidget(
        MaterialApp(home: GamePage(controller: game, initialize: false)),
      );
      final letter = (game.state!['rack'] as List)[0]['letter'] as String;
      Future<void> finishNativeAction() async {
        for (var attempt = 0; attempt < 200 && game.busy; attempt++) {
          await tester.runAsync(
            () => Future<void>.delayed(const Duration(milliseconds: 10)),
          );
          await tester.pump();
        }
        expect(game.busy, isFalse);
      }

      await dragTo(tester, rack(0), tester.getCenter(cell(119)));
      await finishNativeAction();
      expect((game.state!['board'] as List)[119]['letter'], letter);
      expect((game.state!['board'] as List)[119]['rack'], 0);
      await dragTo(tester, cell(119), tester.getCenter(cell(120)));
      await finishNativeAction();
      expect((game.state!['board'] as List)[119], isNull);
      expect((game.state!['board'] as List)[120]['letter'], letter);
      expect(game.storageWarning, isNull);
      await tester.pumpWidget(const SizedBox());
    },
  );
}
