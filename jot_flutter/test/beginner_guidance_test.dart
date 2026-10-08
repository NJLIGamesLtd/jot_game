import 'dart:async';

import 'package:flutter/material.dart';
import 'package:flutter/semantics.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:jot_flutter/main.dart';
import 'package:shared_preferences/shared_preferences.dart';

import 'support/interactive_fixture.dart';
import 'support/multiplayer_fixture.dart' show room;

final guidance = find.byKey(const ValueKey('turn-guidance'));
final refill = find.byKey(const ValueKey('refill-rack-action'));

/// A presentation fixture for the reported valid-word/short-rack situation.
/// Command acceptance remains authoritative; no rule or scoring code is copied.
class GuidanceFixture extends InteractiveFixture {
  int wireRevision = 11;
  String activeRoom = room;
  int seat = 0;

  GuidanceFixture({bool staged = true}) {
    state!['revision'] = 7;
    state!['robotPlayer'] = -1;
    state!['rack'] = staged
        ? [
            for (final letter in ['J', 'R', 'T', 'E', 'S'])
              {
                'letter': letter,
                'blank': false,
                'placed': letter == 'J' || letter == 'R',
                'score': letter == 'J' ? 8 : 1,
              },
          ]
        : <Map<String, dynamic>>[];
    if (staged) {
      final board = state!['board'] as List;
      board[118] = {'letter': 'A', 'pending': false, 'owner': 0, 'score': 1};
      for (final (index, letter, rackIndex) in [(117, 'J', 0), (119, 'R', 1)]) {
        board[index] = {
          'letter': letter,
          'pending': true,
          'rack': rackIndex,
          'owner': 0,
          'score': letter == 'J' ? 8 : 1,
        };
      }
      state!['canDraw'] = false;
      state!['preview'] = {
        'valid': true,
        'score': 19,
        'message': 'JAR is valid.',
        'words': [
          {'word': 'JAR', 'score': 19},
        ],
      };
    }
  }

  @override
  int get onlineRevision => wireRevision;
  @override
  String? get roomId => online ? activeRoom : null;
  @override
  int? get yourPlayer => online ? seat : null;

  void acceptRecall() {
    final board = state!['board'] as List;
    for (var index = 0; index < board.length; index++) {
      if (board[index] is Map && board[index]['pending'] == true) {
        board[index] = null;
      }
    }
    for (final tile in state!['rack'] as List) {
      (tile as Map)['placed'] = false;
    }
    state!['canDraw'] = true;
    state!['canPlay'] = false;
    state!['revision'] = (state!['revision'] as int) + 1;
    if (online) wireRevision++;
    state!['preview'] = {
      'valid': false,
      'score': 0,
      'message': 'Place tiles first.',
      'words': [],
    };
  }

  void acceptFill() {
    final rack = state!['rack'] as List;
    while (rack.length < 8) {
      rack.add({'letter': 'N', 'blank': false, 'placed': false, 'score': 1});
    }
    state!['revision'] = (state!['revision'] as int) + 1;
    if (online) wireRevision++;
    changed();
  }
}

Future<void> mountGuidance(
  WidgetTester tester,
  GuidanceFixture game, {
  Size size = const Size(1440, 1400),
  double textScale = 1,
}) async {
  tester.view.physicalSize = size;
  tester.view.devicePixelRatio = 1;
  addTearDown(tester.view.resetPhysicalSize);
  addTearDown(tester.view.resetDevicePixelRatio);
  await tester.pumpWidget(
    MaterialApp(
      builder: (context, child) => MediaQuery(
        data: MediaQuery.of(
          context,
        ).copyWith(textScaler: TextScaler.linear(textScale)),
        child: child!,
      ),
      home: GamePage(controller: game, initialize: false),
    ),
  );
  await tester.pump();
}

String guidanceText(WidgetTester tester) => tester
    .widgetList<Text>(
      find.descendant(of: guidance, matching: find.byType(Text)),
    )
    .map((text) => text.data ?? text.textSpan!.toPlainText())
    .join(' ');

void expectNoRefill(WidgetTester tester) {
  if (refill.evaluate().isNotEmpty) {
    final button = tester.widget<ButtonStyleButton>(refill);
    expect(button.onPressed, isNull);
  }
}

Future<void> tapRefill(WidgetTester tester) async {
  await tester.ensureVisible(refill);
  await tester.tap(refill);
  await tester.pump();
}

void main() {
  setUp(() => SharedPreferences.setMockInitialValues({}));

  testWidgets(
    'An empty rack explains drawing eight tiles before placing a word',
    (tester) async {
      final game = GuidanceFixture(staged: false);
      await mountGuidance(tester, game);
      expect(guidance, findsOneWidget);
      expect(guidanceText(tester), contains('Draw your tiles'));
      expect(guidanceText(tester), contains('Draw 8 tiles to fill your rack'));
      expect(
        guidanceText(tester),
        matches(RegExp(r'(8|eight)', caseSensitive: false)),
      );
      expect(
        guidanceText(tester),
        matches(RegExp(r'(draw|fill)', caseSensitive: false)),
      );
      expect(find.text('Fill rack'), findsWidgets);
      expect(game.commands, isEmpty);
    },
  );

  testWidgets(
    'The four first-word centre squares stay highlighted while staging and clear after a committed word',
    (tester) async {
      final semantics = tester.ensureSemantics();
      try {
        final game = GuidanceFixture(staged: false);
        await mountGuidance(tester, game);
        const centres = [119, 120, 135, 136];
        for (final index in centres) {
          expect(
            find.byKey(ValueKey('first-move-centre-$index')),
            findsOneWidget,
          );
        }
        final firstCell = tester.getSemantics(
          find.byKey(const ValueKey('board-cell-119')),
        );
        expect(firstCell.label, 'Row 8, column 8, empty');
        expect(
          firstCell.getSemanticsData().hint,
          matches(RegExp(r'cent(re|er)', caseSensitive: false)),
        );
        (game.state!['rack'] as List).add({
          'letter': 'A',
          'blank': false,
          'placed': true,
          'score': 1,
        });
        (game.state!['board'] as List)[119] = {
          'letter': 'A',
          'pending': true,
          'rack': 0,
          'owner': 0,
          'score': 1,
        };
        game.state!['canDraw'] = false;
        game.changed();
        await tester.pump();
        for (final index in centres) {
          expect(
            find.byKey(ValueKey('first-move-centre-$index')),
            findsOneWidget,
          );
        }
        ((game.state!['board'] as List)[119] as Map)['pending'] = false;
        game.changed();
        await tester.pump();
        for (final index in centres) {
          expect(
            find.byKey(ValueKey('first-move-centre-$index')),
            findsNothing,
          );
        }
        expect(tester.takeException(), isNull);
      } finally {
        semantics.dispose();
      }
    },
  );

  for (final empty in [true, false]) {
    testWidgets(
      'The ${empty ? 'empty' : 'short'} unstaged rack fills directly without recalling or playing',
      (tester) async {
        final game = GuidanceFixture(staged: !empty);
        if (!empty) game.acceptRecall();
        game.action = (op) async {
          if (op == 'fill') game.acceptFill();
        };
        await mountGuidance(tester, game);
        expect(refill, findsOneWidget);
        expect(
          find.text(empty ? 'Fill rack to begin' : 'Refill to 8 tiles'),
          findsOneWidget,
        );
        await tapRefill(tester);
        await tester.pump();
        expect(game.commands, [
          {'op': 'fill'},
        ]);
        expect(game.state!['rack'], hasLength(8));
        expect(game.state!['preview']['words'], isEmpty);
      },
    );
  }

  testWidgets('An old direct-fill callback respects a newly busy game', (
    tester,
  ) async {
    final game = GuidanceFixture(staged: false);
    await mountGuidance(tester, game);
    final callback = tester.widget<ButtonStyleButton>(refill).onPressed!;
    game.busy = true;
    callback();
    await tester.pump();
    expect(game.commands, isEmpty);
    expect(tester.takeException(), isNull);
  });

  testWidgets(
    'Valid JAR at 19 points with five rack tiles explains the missing three and offers recovery',
    (tester) async {
      final game = GuidanceFixture();
      await mountGuidance(tester, game);
      expect(find.text('JAR'), findsOneWidget);
      expect(find.text('19'), findsOneWidget);
      expect(
        guidanceText(tester),
        matches(RegExp(r'(3|three)', caseSensitive: false)),
      );
      expect(guidanceText(tester), contains('valid for 19 points'));
      expect(guidanceText(tester), contains('Return the placed tiles'));
      expect(guidanceText(tester), contains('draw 3 more tiles to reach 8'));
      expect(refill, findsOneWidget);
      expect(find.text('Return tiles & fill rack'), findsOneWidget);
      expect(tester.widget<ButtonStyleButton>(refill).onPressed, isNotNull);
      expect(
        tester
            .widget<FilledButton>(
              find.widgetWithText(FilledButton, 'Play word'),
            )
            .onPressed,
        isNull,
      );
    },
  );

  testWidgets('Recovery recalls then fills without replaying the staged word', (
    tester,
  ) async {
    final game = GuidanceFixture();
    game.action = (op) async {
      if (op == 'recall') game.acceptRecall();
      if (op == 'fill') game.acceptFill();
    };
    await mountGuidance(tester, game);
    await tapRefill(tester);
    await tester.pump();
    expect(game.commands, [
      {'op': 'recall'},
      {'op': 'fill'},
    ]);
    expect(game.state!['rack'], hasLength(8));
    expect((game.state!['board'] as List)[118]['letter'], 'A');
    expect((game.state!['board'] as List)[117], isNull);
    expect((game.state!['board'] as List)[119], isNull);
    expect(game.state!['canPlay'], isFalse);
    expect(game.state!['preview']['words'], isEmpty);
    expect(tester.takeException(), isNull);
  });

  for (final interruption in [
    'failed recall',
    'turn change',
    'room change',
    'seat change',
    'mode change',
    'rejected recall with refreshed board',
    'stale accepted reply',
    'disconnected',
    'unmounted',
  ]) {
    testWidgets('Recovery does not fill after $interruption', (tester) async {
      final game = GuidanceFixture()
        ..online = true
        ..connected = true;
      final held = Completer<void>();
      game.action = (_) => held.future;
      await mountGuidance(tester, game);
      await tapRefill(tester);
      expect(game.commands, [
        {'op': 'recall'},
      ]);
      if (interruption != 'failed recall') game.acceptRecall();
      switch (interruption) {
        case 'failed recall':
          game.message = 'The tiles could not be returned.';
          game.actionAccepted = false;
        case 'turn change':
          game.state!['currentPlayer'] = 1;
        case 'room change':
          game.activeRoom = 'abcdef0123456789abcdef0123456789';
        case 'seat change':
          game.seat = 1;
          game.state!['currentPlayer'] = 1;
        case 'mode change':
          game.online = false;
        case 'rejected recall with refreshed board':
          game.actionAccepted = false;
          game.message = 'The game changed on another device.';
        case 'stale accepted reply':
          game.state!['revision'] = (game.state!['revision'] as int) + 1;
          game.wireRevision++;
        case 'disconnected':
          game.connected = false;
        case 'unmounted':
          await tester.pumpWidget(const SizedBox());
      }
      held.complete();
      await tester.pump();
      expect(game.commands, [
        {'op': 'recall'},
      ]);
      expect(tester.takeException(), isNull);
    });
  }

  testWidgets('Empty bags allow a short rack to proceed with its valid word', (
    tester,
  ) async {
    final game = GuidanceFixture();
    game.state!['remaining'] = [
      {'vowels': 0, 'consonants': 0},
      {'vowels': 0, 'consonants': 0},
    ];
    game.state!['canPlay'] = true;
    await mountGuidance(tester, game);
    expectNoRefill(tester);
    expect(guidanceText(tester), contains('Play your word'));
    expect(
      guidanceText(tester),
      matches(RegExp(r'(play|ready)', caseSensitive: false)),
    );
    expect(
      tester
          .widget<FilledButton>(find.widgetWithText(FilledButton, 'Play word'))
          .onPressed,
      isNotNull,
    );
  });

  testWidgets('A full rack and valid word explain playing without refilling', (
    tester,
  ) async {
    final game = GuidanceFixture();
    for (var index = 0; index < 3; index++) {
      (game.state!['rack'] as List).add({
        'letter': 'N',
        'blank': false,
        'placed': false,
        'score': 1,
      });
    }
    game.state!['canPlay'] = true;
    await mountGuidance(tester, game);
    expectNoRefill(tester);
    expect(guidanceText(tester), contains('This word is worth 19 points'));
    expect(
      guidanceText(tester),
      matches(RegExp(r'Play', caseSensitive: false)),
    );
    final play = find.widgetWithText(FilledButton, 'Play word');
    await tester.ensureVisible(play);
    await tester.tap(play);
    await tester.pump();
    expect(game.commands, [
      {'op': 'play'},
    ]);
  });

  for (final mode in ['waiting', 'reconnect', 'busy', 'over']) {
    testWidgets('$mode guidance does not offer an actionable refill', (
      tester,
    ) async {
      final game = GuidanceFixture();
      switch (mode) {
        case 'waiting':
          game.online = true;
          game.connected = true;
          game.state!['currentPlayer'] = 1;
        case 'reconnect':
          game.online = true;
          game.connected = false;
          game.connectionError = 'Connection interrupted.';
        case 'busy':
          game.busy = true;
        case 'over':
          game.state!['over'] = true;
      }
      await mountGuidance(tester, game);
      expect(guidance, findsOneWidget);
      expect(
        guidanceText(tester),
        contains(switch (mode) {
          'waiting' => 'Your friend’s turn',
          'reconnect' => 'Reconnecting',
          'busy' => 'One moment',
          _ => 'Game over',
        }),
      );
      expectNoRefill(tester);
      expect(game.commands, isEmpty);
      expect(tester.takeException(), isNull);
    });
  }

  for (final online in [false, true]) {
    testWidgets(
      'Retry with an existing failed ${online ? 'online' : 'local'} snapshot reaches the correct recovery path',
      (tester) async {
        final game = GuidanceFixture()
          ..online = online
          ..failed = true
          ..message = 'The last request could not finish.';
        await mountGuidance(tester, game);
        expect(game.state, isNotNull);
        expectNoRefill(tester);
        final retry = find.widgetWithText(TextButton, 'Retry');
        expect(retry, findsOneWidget);
        expect(tester.widget<TextButton>(retry).onPressed, isNotNull);
        await tester.ensureVisible(retry);
        await tester.tap(retry);
        await tester.pump();
        expect(game.initialized, online ? 0 : 1);
        expect(game.retries, online ? 1 : 0);
        expect(game.commands, isEmpty);
        expect(tester.takeException(), isNull);
      },
    );
  }

  testWidgets('Retry with an existing failed snapshot is disabled while busy', (
    tester,
  ) async {
    final game = GuidanceFixture()
      ..failed = true
      ..busy = true;
    await mountGuidance(tester, game);
    expect(game.state, isNotNull);
    expectNoRefill(tester);
    final retry = find.widgetWithText(TextButton, 'Retry');
    expect(retry, findsOneWidget);
    expect(tester.widget<TextButton>(retry).onPressed, isNull);
    await tester.ensureVisible(retry);
    await tester.tap(retry);
    await tester.pump();
    expect(game.initialized, 0);
    expect(game.retries, 0);
    expect(game.commands, isEmpty);
    expect(tester.takeException(), isNull);
  });

  for (final size in [
    const Size(360, 780),
    const Size(820, 1180),
    const Size(1440, 1000),
  ]) {
    for (final textScale in [1.0, 2.0]) {
      testWidgets(
        'Guidance and recovery fit ${size.width}px at text scale $textScale',
        (tester) async {
          final game = GuidanceFixture();
          await mountGuidance(tester, game, size: size, textScale: textScale);
          await tester.ensureVisible(guidance);
          await tester.pump();
          expect(guidance, findsOneWidget);
          expect(refill, findsOneWidget);
          final card = tester.getRect(guidance);
          expect(card.left, greaterThanOrEqualTo(0));
          expect(card.right, lessThanOrEqualTo(size.width));
          expect(tester.getRect(refill).width, lessThanOrEqualTo(card.width));
          await tester.ensureVisible(refill);
          await tester.pump();
          final recoveryTarget = tester.getCenter(refill);
          expect(recoveryTarget.dx, inInclusiveRange(0, size.width));
          expect(recoveryTarget.dy, inInclusiveRange(0, size.height));
          expect(tester.takeException(), isNull);
        },
      );
    }
  }

  testWidgets('The help guide is accessible, explains controls and dismisses', (
    tester,
  ) async {
    final semantics = tester.ensureSemantics();
    try {
      final game = GuidanceFixture(staged: false);
      await mountGuidance(
        tester,
        game,
        size: const Size(360, 780),
        textScale: 2,
      );
      final help = find.byTooltip('How to play');
      final helpNode = tester.getSemantics(help);
      expect(
        helpNode.getSemanticsData().hasAction(SemanticsAction.tap),
        isTrue,
      );
      helpNode.owner!.performAction(helpNode.id, SemanticsAction.tap);
      await tester.pumpAndSettle();
      expect(find.byType(AlertDialog), findsOneWidget);
      expect(
        find.descendant(
          of: find.byType(AlertDialog),
          matching: find.text('How to play'),
        ),
        findsOneWidget,
      );
      final explanation = tester
          .widgetList<Text>(
            find.descendant(
              of: find.byType(AlertDialog),
              matching: find.byType(Text),
            ),
          )
          .map((text) => text.data ?? '')
          .join(' ');
      for (final topic in [
        r'(8|eight)',
        r'(refill|fill rack)',
        r'drag',
        r'cent(re|er)',
        r'connect',
        r'Play',
        r'Recall',
        r'Hint',
        r'Shuffle',
        r'Pass',
      ]) {
        expect(explanation, matches(RegExp(topic, caseSensitive: false)));
      }
      final gotIt = find.widgetWithText(TextButton, 'Got it');
      await tester.ensureVisible(gotIt);
      final dismiss = tester.getSemantics(gotIt);
      expect(dismiss.getSemanticsData().hasAction(SemanticsAction.tap), isTrue);
      dismiss.owner!.performAction(dismiss.id, SemanticsAction.tap);
      await tester.pumpAndSettle();
      expect(find.byType(AlertDialog), findsNothing);
      expect(game.commands, isEmpty);
      expect(tester.takeException(), isNull);
    } finally {
      semantics.dispose();
    }
  });
}
