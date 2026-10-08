import 'dart:convert';

import 'package:flutter/material.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:jot_core/jot_core.dart';
import 'package:jot_flutter/game_controller.dart';
import 'package:jot_flutter/main.dart';
import 'package:shared_preferences/shared_preferences.dart';

const _dictionary = 'EARNING\nPATE\nJAR\n';
const _jCell = 6 * 16 + 2;
const _aCell = 6 * 16 + 3;
const _rCell = 6 * 16 + 4;

Future<Map<String, dynamic>> _command(
  CoreTransport core,
  Map<String, dynamic> command,
) async {
  final result =
      jsonDecode(await core.execute(jsonEncode(command)))
          as Map<String, dynamic>;
  expect(result['ok'], isTrue, reason: '$command: ${result['message']}');
  return result;
}

/// Selects deterministic draws without inventing or transferring any tiles.
void _prepareRack(Map fullState, int owner, String letters) {
  final player = (fullState['players'] as List)[owner] as Map;
  for (final letter in letters.split('')) {
    final bagName = 'AEIOU@'.contains(letter) ? 'vowels' : 'consonants';
    final bag = player[bagName] as String;
    final index = bag.indexOf(letter);
    expect(index, isNonNegative, reason: 'Player $owner needs $letter.');
    player[bagName] = bag.replaceRange(index, index + 1, '');
    (player['rack'] as List).add({
      'letter': letter,
      'blank': letter == '@',
      'owner': owner,
    });
  }
}

/// The C++ engine commits EARNING and PATE before staging the reported JAR
/// draft. Every restore verifies the original per-player tile inventory.
Future<String> _shortRackDraft() async {
  final core = await createCore(_dictionary, seed: 314159, robot: -1);
  try {
    var save = (await _command(core, {'op': 'save'}))['save'] as Map;
    var fullState = save['fullState'] as Map;
    fullState['currentPlayer'] = 1;
    _prepareRack(fullState, 0, 'JRABC');
    _prepareRack(fullState, 1, 'EARNINGB');
    await _command(core, {'op': 'restore', 'save': save});
    for (var index = 0; index < 'EARNING'.length; index++) {
      await _command(core, {
        'op': 'place',
        'rack': index,
        'row': 8,
        'col': 3 + index,
        'letter': 'EARNING'[index],
      });
    }
    await _command(core, {'op': 'play'});
    await _command(core, {'op': 'pass'});

    save = (await _command(core, {'op': 'save'}))['save'] as Map;
    fullState = save['fullState'] as Map;
    _prepareRack(fullState, 1, 'PAT');
    await _command(core, {'op': 'restore', 'save': save});
    await _command(core, {'op': 'fill'});
    for (var index = 0; index < 'PAT'.length; index++) {
      await _command(core, {
        'op': 'place',
        'rack': index + 1,
        'row': 5 + index,
        'col': 3,
        'letter': 'PAT'[index],
      });
    }
    await _command(core, {'op': 'play'});
    await _command(core, {
      'op': 'place',
      'rack': 0,
      'row': 6,
      'col': 2,
      'letter': 'J',
    });
    final staged = await _command(core, {
      'op': 'place',
      'rack': 1,
      'row': 6,
      'col': 4,
      'letter': 'R',
    });
    final state = staged['state'] as Map;
    expect(state['currentPlayer'], 0);
    expect(state['robotPlayer'], -1);
    expect(state['rack'], hasLength(5));
    expect((state['preview'] as Map)['valid'], isTrue);
    expect((state['preview'] as Map)['score'], 19);
    expect(state['canPlay'], isFalse);
    return jsonEncode((await _command(core, {'op': 'save'}))['save']);
  } finally {
    await core.close();
  }
}

Finder _rack(int index) => find.byKey(ValueKey('rack-tile-$index'));
Finder _cell(int index) => find.byKey(ValueKey('board-cell-$index'));

void main() {
  testWidgets(
    'Short-rack JAR guidance recalls only drafts, fills eight tiles and commits the actual 19-point word',
    (tester) async {
      tester.view.physicalSize = const Size(1440, 1400);
      tester.view.devicePixelRatio = 1;
      addTearDown(tester.view.resetPhysicalSize);
      addTearDown(tester.view.resetDevicePixelRatio);
      final saved = await tester.runAsync(_shortRackDraft);
      expect(saved, isNotNull);
      SharedPreferences.setMockInitialValues({GameController.saveKey: saved!});
      final game = GameController(
        dictionaryLoader: () async => _dictionary,
        coreFactory: (dictionary) =>
            createCore(dictionary, seed: 314159, robot: -1),
      );

      Future<void> waitForEngine({bool Function()? complete}) async {
        for (
          var attempt = 0;
          attempt < 300 && (game.busy || !(complete?.call() ?? true));
          attempt++
        ) {
          await tester.runAsync(
            () => Future<void>.delayed(const Duration(milliseconds: 10)),
          );
          await tester.pump();
        }
        expect(game.busy, isFalse, reason: game.message);
        expect(game.failed, isFalse, reason: game.message);
        expect(complete?.call() ?? true, isTrue, reason: game.message);
      }

      await tester.pumpWidget(MaterialApp(home: GamePage(controller: game)));
      await waitForEngine();
      final before = jsonDecode(jsonEncode(game.state)) as Map;
      final beforeBoard = before['board'] as List;
      final beforeScores = before['scores'] as List;
      expect((before['preview'] as Map)['valid'], isTrue);
      expect((before['preview'] as Map)['score'], 19);
      expect(before['rack'], hasLength(5));
      expect(before['canPlay'], isFalse);
      expect(before['canDraw'], isFalse);
      expect(beforeBoard[_aCell]['letter'], 'A');
      expect(beforeBoard[_aCell]['pending'], isFalse);
      expect(
        (before['remaining'] as List)[0]['vowels'] +
            (before['remaining'] as List)[0]['consonants'],
        greaterThan(0),
      );
      final play = find.widgetWithText(FilledButton, 'Play word');
      expect(tester.widget<FilledButton>(play).onPressed, isNull);
      expect(find.byKey(const ValueKey('turn-guidance')), findsOneWidget);
      final refill = find.byKey(const ValueKey('refill-rack-action'));
      expect(find.text('Return tiles & fill rack'), findsOneWidget);
      expect(tester.widget<ButtonStyleButton>(refill).onPressed, isNotNull);

      await tester.tap(_rack(4));
      await tester.pump();
      expect(tester.widget<RackTileFace>(_rack(4)).selected, isTrue);
      await tester.ensureVisible(refill);
      await tester.tap(refill);
      await tester.pump();
      await waitForEngine(
        complete: () => (game.state?['rack'] as List?)?.length == 8,
      );
      final refilled = game.state!;
      expect(refilled['currentPlayer'], 0);
      expect(refilled['scores'], beforeScores);
      expect(refilled['rack'], hasLength(8));
      expect(
        (refilled['rack'] as List).every((t) => t['placed'] == false),
        isTrue,
      );
      expect(refilled['canDraw'], isFalse);
      expect(tester.widget<RackTileFace>(_rack(4)).selected, isFalse);
      final refilledBoard = refilled['board'] as List;
      expect(refilledBoard[_jCell], isNull);
      expect(refilledBoard[_rCell], isNull);
      for (var index = 0; index < beforeBoard.length; index++) {
        final tile = beforeBoard[index];
        if (tile is Map && tile['pending'] == false) {
          expect(refilledBoard[index], tile, reason: 'Committed cell $index');
        }
      }

      final pointer = await tester.startGesture(tester.getCenter(_rack(0)));
      await pointer.moveBy(const Offset(25, -25));
      await tester.pump();
      await pointer.moveTo(tester.getCenter(_cell(_jCell)));
      await tester.pump();
      await pointer.up();
      await tester.pump();
      await waitForEngine();
      expect((game.state!['board'] as List)[_jCell]['rack'], 0);

      await tester.tap(_rack(1));
      await tester.pump();
      await tester.tap(_cell(_rCell));
      await tester.pump();
      await waitForEngine();
      expect((game.state!['preview'] as Map)['valid'], isTrue);
      expect((game.state!['preview'] as Map)['score'], 19);
      expect(game.state!['canPlay'], isTrue);
      expect(tester.widget<FilledButton>(play).onPressed, isNotNull);
      await tester.ensureVisible(play);
      await tester.tap(play);
      await tester.pump();
      await waitForEngine();

      expect(game.state!['currentPlayer'], 1);
      expect((game.state!['scores'] as List)[0], (beforeScores[0] as int) + 19);
      expect((game.state!['scores'] as List)[1], beforeScores[1]);
      for (final (index, letter) in [(_jCell, 'J'), (_rCell, 'R')]) {
        final tile = (game.state!['board'] as List)[index] as Map;
        expect(tile['letter'], letter);
        expect(tile['pending'], isFalse);
        expect(tile['owner'], 0);
      }
      expect((game.state!['board'] as List)[_aCell], beforeBoard[_aCell]);
      final preferences = await SharedPreferences.getInstance();
      final committed = jsonDecode(
        preferences.getString(GameController.saveKey)!,
      );
      final fullState = committed['fullState'] as Map;
      expect(fullState['pending'], isEmpty);
      expect(fullState['scores'], game.state!['scores']);
      expect((fullState['players'] as List)[0]['rack'], hasLength(6));
      expect(game.storageWarning, isNull);
      expect(tester.takeException(), isNull);
      await tester.pumpWidget(const SizedBox());
    },
  );
}
