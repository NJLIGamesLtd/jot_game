import 'dart:convert';

import 'package:flutter/material.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:jot_core/jot_core.dart';
import 'package:jot_flutter/game_controller.dart';
import 'package:jot_flutter/main.dart';
import 'package:shared_preferences/shared_preferences.dart';

/// Runs the actual Flutter presentation -> controller -> native isolate -> C++
/// engine pipeline. Only the device preferences plugin is replaced in memory.
void main() {
  testWidgets(
    'complete local UI game plays a legal word, receives a computer reply and resumes after reload',
    (tester) async {
      SharedPreferences.setMockInitialValues({});
      tester.view.physicalSize = const Size(1440, 1400);
      tester.view.devicePixelRatio = 1;
      addTearDown(tester.view.resetPhysicalSize);
      addTearDown(tester.view.resetDevicePixelRatio);

      Future<void> waitForEngine(GameController game) async {
        for (var attempt = 0; attempt < 300 && game.busy; attempt++) {
          await tester.runAsync(
            () => Future<void>.delayed(const Duration(milliseconds: 10)),
          );
          await tester.pump();
        }
        expect(game.busy, isFalse, reason: game.message);
        expect(game.failed, isFalse, reason: game.message);
      }

      final game = GameController(
        coreFactory: (dictionary) => createCore(dictionary, seed: 12345),
      );
      await tester.pumpWidget(MaterialApp(home: GamePage(controller: game)));
      await waitForEngine(game);
      expect(find.byType(JotBoard), findsOneWidget);

      for (final label in ['Fill rack', 'Hint', 'Place hint', 'Play word']) {
        final button = find.text(label);
        await tester.ensureVisible(button);
        await tester.tap(button);
        await tester.pump();
        await waitForEngine(game);
      }
      expect(
        (game.state!['scores'] as List).every((score) => (score as int) > 0),
        isTrue,
      );
      expect(game.state!['currentPlayer'], 0);
      expect((game.state!['board'] as List).whereType<Map>(), isNotEmpty);
      final saved = jsonEncode(game.state);
      await tester.pumpWidget(const SizedBox());
      await tester.pump();

      final resumed = GameController();
      await tester.pumpWidget(MaterialApp(home: GamePage(controller: resumed)));
      await waitForEngine(resumed);
      expect(jsonEncode(resumed.state), saved);
      expect(find.byType(JotBoard), findsOneWidget);
      expect(resumed.storageWarning, isNull);
      expect(tester.takeException(), isNull);
      await tester.pumpWidget(const SizedBox());
    },
  );
}
