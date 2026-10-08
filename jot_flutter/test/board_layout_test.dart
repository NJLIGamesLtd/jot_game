import 'package:flutter/foundation.dart';
import 'package:flutter/material.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:jot_flutter/main.dart';

import 'support/presentation_fixture.dart';

const desktopViewport = Size(1280, 720);

void expectInside(Rect child, Rect parent) {
  const tolerance = .01;
  expect(child.left, greaterThanOrEqualTo(parent.left - tolerance));
  expect(child.top, greaterThanOrEqualTo(parent.top - tolerance));
  expect(child.right, lessThanOrEqualTo(parent.right + tolerance));
  expect(child.bottom, lessThanOrEqualTo(parent.bottom + tolerance));
}

Future<void> verifyAllRowsFit(WidgetTester tester) async {
  tester.view.physicalSize = desktopViewport;
  tester.view.devicePixelRatio = 1;
  addTearDown(tester.view.resetPhysicalSize);
  addTearDown(tester.view.resetDevicePixelRatio);
  final game = PresentationFixture();
  await tester.pumpWidget(
    MaterialApp(home: GamePage(controller: game, initialize: false)),
  );
  await tester.pump();

  final board = find.byType(JotBoard);
  final lastCell = find.byKey(const ValueKey('board-cell-255'));
  expect(board, findsOneWidget);
  final boardRect = tester.getRect(board);
  expect(
    boardRect.width,
    closeTo(boardRect.height, .01),
    reason: 'Wide layouts must loosen width so the board remains square.',
  );
  expectInside(boardRect, Offset.zero & desktopViewport);
  expect(
    lastCell,
    findsOneWidget,
    reason: 'The sixteenth row must be built without scrolling or zooming.',
  );
  final lastRect = tester.getRect(lastCell);
  expectInside(lastRect, boardRect);
  expectInside(lastRect, Offset.zero & desktopViewport);

  // A visible cell must also receive pointer input at the expected coordinates.
  await tester.tap(find.byKey(const ValueKey('rack-tile-0')));
  await tester.tap(lastCell);
  await tester.pump();
  expect(game.operation, 'place');
  expect(game.fields, {'rack': 0, 'row': 15, 'col': 15, 'letter': 'A'});
  expect(tester.takeException(), isNull);
}

void main() {
  // Overrides exercise shared layout defaults; they do not emulate native OSes.
  if (kIsWeb) {
    testWidgets(
      'Chrome 1280×720 shows a square board and the complete last row',
      verifyAllRowsFit,
    );
  } else {
    for (final platform in [
      TargetPlatform.android,
      TargetPlatform.iOS,
      TargetPlatform.windows,
      TargetPlatform.macOS,
      TargetPlatform.linux,
    ]) {
      testWidgets(
        '${platform.name} 1280×720 shows a square board and the complete last row',
        verifyAllRowsFit,
        variant: TargetPlatformVariant({platform}),
      );
    }
  }
}
