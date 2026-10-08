import 'package:flutter/material.dart';
import 'package:flutter/semantics.dart';
import 'package:flutter/services.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:jot_flutter/main.dart' as jot;
import 'package:shared_preferences/shared_preferences.dart';

import 'support/multiplayer_fixture.dart' show room;
import 'support/interactive_fixture.dart';

Future<void> mount(
  WidgetTester tester,
  InteractiveFixture game, {
  bool initialize = false,
}) async {
  tester.view.physicalSize = const Size(1440, 1400);
  tester.view.devicePixelRatio = 1;
  addTearDown(tester.view.resetPhysicalSize);
  addTearDown(tester.view.resetDevicePixelRatio);
  await tester.pumpWidget(
    MaterialApp(
      home: jot.GamePage(controller: game, initialize: initialize),
    ),
  );
  await tester.pump();
}

Future<void> tapText(WidgetTester tester, String label) async {
  final target = find.text(label).last;
  await tester.ensureVisible(target);
  await tester.tap(target);
  await tester.pumpAndSettle();
}

void main() {
  setUp(() => SharedPreferences.setMockInitialValues({}));

  testWidgets(
    'A stale tile selection is cleared when its rack disappears before placement',
    (tester) async {
      final game = InteractiveFixture();
      final semantics = tester.ensureSemantics();
      await mount(tester, game);
      await tester.tap(find.bySemanticsLabel('Rack tile 1, A'));
      (game.state!['rack'] as List).clear();
      await tester.tap(find.bySemanticsLabel('Row 8, column 8, empty'));
      await tester.pump();
      expect(game.commands, isEmpty);
      expect(find.textContaining('YOUR RACK  ·  0/8'), findsOneWidget);
      semantics.dispose();
    },
  );

  testWidgets(
    'Assistive-technology tile and board actions use the same placement path',
    (tester) async {
      final game = InteractiveFixture();
      final semantics = tester.ensureSemantics();
      await mount(tester, game);
      final rack = tester.getSemantics(find.bySemanticsLabel('Rack tile 1, A'));
      rack.owner!.performAction(rack.id, SemanticsAction.tap);
      await tester.pump();
      final board = tester.getSemantics(
        find.bySemanticsLabel('Row 8, column 8, empty'),
      );
      board.owner!.performAction(board.id, SemanticsAction.tap);
      await tester.pump();
      expect(game.commands.single, {
        'op': 'place',
        'rack': 0,
        'row': 7,
        'col': 7,
        'letter': 'A',
      });
      semantics.dispose();
    },
  );

  testWidgets(
    'A late board gesture while the controller turns busy cannot place a tile',
    (tester) async {
      final game = InteractiveFixture();
      final semantics = tester.ensureSemantics();
      await mount(tester, game);
      await tester.tap(find.bySemanticsLabel('Rack tile 1, A'));
      game.busy = true;
      await tester.tap(find.bySemanticsLabel('Row 8, column 8, empty'));
      await tester.pump();
      expect(game.commands, isEmpty);
      semantics.dispose();
    },
  );

  testWidgets(
    'A stale copy button tolerates an invite disappearing before the click',
    (tester) async {
      final game = InteractiveFixture()
        ..online = true
        ..hasInvite = true;
      await mount(tester, game);
      game.hasInvite = false;
      await tapText(tester, 'Copy invite link');
      expect(find.byType(SnackBar), findsNothing);
    },
  );

  testWidgets(
    'Waiting-room, busy and joining presentation states fit a narrow viewport',
    (tester) async {
      final game = InteractiveFixture()
        ..online = true
        ..roomJoined = false
        ..hasInvite = true
        ..busy = true;
      await mount(tester, game);
      tester.view.physicalSize = const Size(390, 844);
      game.changed();
      await tester.pump();
      expect(find.text('Waiting for your friend to join.'), findsOneWidget);
      expect(find.byType(CircularProgressIndicator), findsOneWidget);
      game.online = false;
      game.joining = true;
      game.changed();
      await tester.pump();
      expect(find.text('Connecting to online game'), findsOneWidget);
      expect(tester.takeException(), isNull);
    },
  );

  testWidgets(
    'Application entry boots the real C++ engine and renders themed game UI',
    (tester) async {
      await tester.runAsync(jot.main);
      for (
        var attempt = 0;
        attempt < 100 && find.byType(jot.JotBoard).evaluate().isEmpty;
        attempt++
      ) {
        await tester.runAsync(
          () => Future<void>.delayed(const Duration(milliseconds: 10)),
        );
        await tester.pump();
      }
      expect(find.byType(jot.JotApp), findsOneWidget);
      expect(find.byType(jot.JotBoard), findsOneWidget);
      expect(find.text('jot'), findsOneWidget);
      await tester.pumpWidget(const SizedBox());
    },
  );

  testWidgets(
    'Every enabled gameplay button sends the exact authoritative command',
    (tester) async {
      final game = InteractiveFixture();
      game.state!['canPlay'] = true;
      game.state!['canHint'] = true;
      game.state!['preview'] = {
        'valid': true,
        'score': 12,
        'words': [
          {'word': 'CAT'},
        ],
      };
      game.hint = {'word': 'CAT', 'score': 12};
      await mount(tester, game, initialize: true);
      expect(game.initialized, 1);
      for (final label in [
        'Vowel · 26',
        'Consonant · 37',
        'Fill rack',
        'Play word',
        'Recall',
        'Shuffle',
        'Hint',
        'Pass',
        'Place hint',
      ]) {
        await tapText(tester, label);
      }
      expect(game.commands, [
        {'op': 'draw', 'bag': 'vowel'},
        {'op': 'draw', 'bag': 'consonant'},
        {'op': 'fill'},
        {'op': 'play'},
        {'op': 'recall'},
        {'op': 'shuffle'},
        {'op': 'hint'},
        {'op': 'pass'},
        {'op': 'hint', 'apply': true},
      ]);
      await tapText(tester, 'Online game');
      expect(game.created, 1);
      expect(tester.takeException(), isNull);
    },
  );

  testWidgets(
    'Blank letter dialog stages the chosen letter and can be dismissed safely',
    (tester) async {
      final game = InteractiveFixture();
      (game.state!['rack'] as List)[0] = {
        'letter': '@',
        'blank': true,
        'placed': false,
        'score': 0,
      };
      final semantics = tester.ensureSemantics();
      await mount(tester, game);
      await tester.tap(find.bySemanticsLabel('Rack tile 1, @'));
      await tester.tap(find.bySemanticsLabel('Row 8, column 8, empty'));
      await tester.pumpAndSettle();
      expect(find.text('Choose a letter for your blank'), findsOneWidget);
      await tester.tap(find.widgetWithText(OutlinedButton, 'Z'));
      await tester.pumpAndSettle();
      expect(game.commands.single, {
        'op': 'place',
        'rack': 0,
        'row': 7,
        'col': 7,
        'letter': 'Z',
      });
      await tester.tap(find.bySemanticsLabel('Rack tile 1, @'));
      await tester.tap(find.bySemanticsLabel('Row 8, column 9, empty'));
      await tester.pumpAndSettle();
      await tester.tapAt(const Offset(10, 10));
      await tester.pumpAndSettle();
      expect(game.commands, hasLength(1));
      semantics.dispose();
    },
  );

  testWidgets(
    'Pending tiles return by their rack index, with recall fallback for older snapshots',
    (tester) async {
      final game = InteractiveFixture();
      final board = game.state!['board'] as List;
      board[117] = {'letter': 'C', 'pending': false, 'owner': 0, 'score': 3};
      board[118] = {'letter': 'D', 'pending': false, 'owner': 1, 'score': 2};
      board[119] = {
        'letter': 'A',
        'pending': true,
        'rack': 0,
        'owner': 0,
        'score': 1,
      };
      board[120] = {'letter': 'B', 'pending': true, 'owner': 1, 'score': 3};
      final semantics = tester.ensureSemantics();
      await mount(tester, game);
      await tester.tap(find.bySemanticsLabel('Row 8, column 8, A'));
      await tester.pump();
      await tester.tap(find.bySemanticsLabel('Row 8, column 9, B'));
      await tester.pump();
      expect(game.commands, [
        {'op': 'unplace', 'rack': 0},
        {'op': 'recall'},
      ]);
      semantics.dispose();
    },
  );

  testWidgets(
    'Rules dialog closes and game chooser cancellation leaves the game alone',
    (tester) async {
      final game = InteractiveFixture();
      await mount(tester, game);
      await tester.tap(find.byTooltip('How to play'));
      await tester.pumpAndSettle();
      expect(find.widgetWithText(AlertDialog, 'How to play'), findsOneWidget);
      await tapText(tester, 'Got it');
      await tapText(tester, 'New game');
      await tapText(tester, 'Cancel');
      expect(game.commands, isEmpty);
    },
  );

  for (final option in [
    'Play computer',
    'Two players on this device',
    'Online game',
  ]) {
    testWidgets('Game chooser routes $option correctly', (tester) async {
      final game = InteractiveFixture();
      await mount(tester, game);
      await tapText(tester, 'New game');
      await tapText(tester, option);
      if (option == 'Online game') {
        expect(game.created, 1);
      } else {
        expect(game.commands.single['op'], 'new_game');
        expect(game.commands.single['seed'], inInclusiveRange(0, 0xffffffff));
        expect(
          game.commands.single['robot'],
          option == 'Play computer' ? 1 : -1,
        );
      }
    });
  }

  testWidgets(
    'Invite paste dialog joins with the complete link and supports cancel',
    (tester) async {
      final game = InteractiveFixture();
      await mount(tester, game);
      await tapText(tester, 'New game');
      await tapText(tester, 'Join with invite link');
      await tester.enterText(find.byType(TextField), inviteUrlFixture);
      await tapText(tester, 'Join game');
      await tester.pump(const Duration(milliseconds: 350));
      expect(game.joinedLink, inviteUrlFixture);
      await tapText(tester, 'Join with invite link');
      await tapText(tester, 'Cancel');
      await tester.pump(const Duration(milliseconds: 350));
      expect(game.joinedLink, inviteUrlFixture);
    },
  );

  testWidgets('Saved online game can resume from the panel and room chooser', (
    tester,
  ) async {
    final game = InteractiveFixture()..resumable = true;
    await mount(tester, game);
    await tapText(tester, 'Resume online game');
    expect(game.resumedRoom, 'latest');
    await tapText(tester, 'New game');
    await tapText(tester, 'Resume online game · 01234567');
    expect(game.resumedRoom, room);
  });

  testWidgets(
    'Switching online to a local game preserves the intended selection',
    (tester) async {
      final game = InteractiveFixture()
        ..online = true
        ..connected = true;
      game.state!['robotPlayer'] = -1;
      await mount(tester, game);
      await tapText(tester, 'Games');
      await tapText(tester, 'Return to saved local game');
      expect(game.returns, 1);
      expect(game.commands, isEmpty);
      game.online = true;
      game.changed();
      await tester.pump();
      await tapText(tester, 'Games');
      await tapText(tester, 'Play computer');
      expect(game.returns, 2);
      expect(game.commands.single['robot'], 1);
    },
  );

  for (final clipboardFails in [false, true]) {
    testWidgets(
      'Copy invite ${clipboardFails ? 'fallback' : 'success'} preserves the complete guest link',
      (tester) async {
        final game = InteractiveFixture()
          ..online = true
          ..hasInvite = true
          ..connected = true;
        game.state!['robotPlayer'] = -1;
        String? copied;
        tester.binding.defaultBinaryMessenger.setMockMethodCallHandler(
          SystemChannels.platform,
          (call) async {
            if (call.method == 'Clipboard.setData') {
              if (clipboardFails) {
                throw PlatformException(code: 'clipboard_denied');
              }
              copied = (call.arguments as Map)['text'] as String;
            }
            return null;
          },
        );
        addTearDown(
          () => tester.binding.defaultBinaryMessenger.setMockMethodCallHandler(
            SystemChannels.platform,
            null,
          ),
        );
        await mount(tester, game);
        await tapText(tester, 'Copy invite link');
        expect(copied, clipboardFails ? isNull : inviteUrlFixture);
        expect(
          find.text(
            clipboardFails
                ? 'Select and copy the invite link shown below.'
                : 'Invite link copied. Send it to your friend.',
          ),
          findsOneWidget,
        );
      },
    );
  }

  testWidgets(
    'Connection, loading, persistence and computer recovery states stay actionable',
    (tester) async {
      final game = InteractiveFixture()
        ..online = true
        ..connectionError = 'Disconnected';
      game.state!['robotPlayer'] = -1;
      game.storageWarning = 'Save unavailable';
      await mount(tester, game);
      await tapText(tester, 'Reconnect');
      expect(game.retries, 1);
      expect(find.text('Save unavailable'), findsOneWidget);
      game.online = false;
      game.state!['currentPlayer'] = 1;
      game.state!['robotPlayer'] = 1;
      game.changed();
      await tester.pump();
      await tapText(tester, 'Retry computer turn');
      expect(game.commands.single['op'], 'robot');
      game.state = null;
      game.failed = true;
      game.changed();
      await tester.pump();
      await tapText(tester, 'Retry');
      expect(game.initialized, 1);
      game.joining = true;
      game.changed();
      await tester.pump();
      await tapText(tester, 'Retry');
      expect(game.retries, 2);
    },
  );
}
