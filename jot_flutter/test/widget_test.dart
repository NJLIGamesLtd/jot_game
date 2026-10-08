import 'package:flutter/material.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:jot_flutter/main.dart';

import 'support/presentation_fixture.dart';

void main() {
  testWidgets('Online guest sees their own rack and bags while waiting', (
    tester,
  ) async {
    tester.view.physicalSize = const Size(390, 844);
    tester.view.devicePixelRatio = 1;
    addTearDown(tester.view.resetPhysicalSize);
    addTearDown(tester.view.resetDevicePixelRatio);
    await tester.pumpWidget(
      MaterialApp(
        home: GamePage(controller: WaitingOnlineFixture(), initialize: false),
      ),
    );
    expect(find.text('You'), findsOneWidget);
    expect(find.text('Friend'), findsOneWidget);
    expect(find.text('Vowel · 10'), findsOneWidget);
    expect(find.text('Consonant · 25'), findsOneWidget);
    expect(
      tester
          .widget<TextButton>(find.widgetWithText(TextButton, 'Fill rack'))
          .onPressed,
      isNull,
    );
    expect(
      tester
          .widget<FilledButton>(find.widgetWithText(FilledButton, 'Play word'))
          .onPressed,
      isNull,
    );
    expect(tester.takeException(), isNull);
  });
  for (final size in [
    const Size(390, 844),
    const Size(822, 878),
    const Size(1440, 900),
  ]) {
    testWidgets('Board and controls fit ${size.width} pixels', (tester) async {
      tester.view.physicalSize = size;
      tester.view.devicePixelRatio = 1;
      addTearDown(tester.view.resetPhysicalSize);
      addTearDown(tester.view.resetDevicePixelRatio);
      await tester.pumpWidget(
        MaterialApp(
          home: GamePage(controller: PresentationFixture(), initialize: false),
        ),
      );
      expect(find.text('Player 1'), findsOneWidget);
      expect(find.text('Computer'), findsOneWidget);
      expect(find.byType(JotBoard), findsOneWidget);
      expect(find.text('Fill rack'), findsOneWidget);
      expect(tester.takeException(), isNull);
    });
  }
  testWidgets('Selecting a rack tile sends its exact board placement to C++', (
    tester,
  ) async {
    tester.view.physicalSize = const Size(1440, 1000);
    tester.view.devicePixelRatio = 1;
    addTearDown(tester.view.resetPhysicalSize);
    addTearDown(tester.view.resetDevicePixelRatio);
    final controller = PresentationFixture();
    final semantics = tester.ensureSemantics();
    try {
      await tester.pumpWidget(
        MaterialApp(home: GamePage(controller: controller, initialize: false)),
      );
      await tester.tap(find.bySemanticsLabel('Rack tile 1, A'));
      await tester.pump();
      await tester.tap(find.bySemanticsLabel('Row 8, column 8, empty'));
      await tester.pump();
      expect(controller.operation, 'place');
      expect(controller.fields, {'rack': 0, 'row': 7, 'col': 7, 'letter': 'A'});
      expect(tester.takeException(), isNull);
    } finally {
      semantics.dispose();
    }
  });
}
