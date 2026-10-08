import 'dart:ui' show PointerDeviceKind;

import 'package:flutter/foundation.dart';
import 'package:flutter/material.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:jot_flutter/main.dart';
import 'package:shared_preferences/shared_preferences.dart';

import 'support/drag_fixture.dart';

// These are shared-widget tests of platform defaults and pointer handling.
// Running them on a host or in Chrome does not emulate a native OS event loop.
const platforms = [
  TargetPlatform.android,
  TargetPlatform.iOS,
  TargetPlatform.windows,
  TargetPlatform.macOS,
  TargetPlatform.linux,
];
const viewports = {
  'phone': Size(360, 780),
  'tablet': Size(820, 1180),
  'desktop': Size(1440, 1000),
};
const pointers = [
  PointerDeviceKind.touch,
  PointerDeviceKind.mouse,
  PointerDeviceKind.stylus,
];

void expectVisible(WidgetTester tester, Finder finder, Size viewport) {
  final rect = tester.getRect(finder);
  expect(rect.left, greaterThanOrEqualTo(0));
  expect(rect.top, greaterThanOrEqualTo(0));
  expect(rect.right, lessThanOrEqualTo(viewport.width));
  expect(rect.bottom, lessThanOrEqualTo(viewport.height));
}

Future<void> exercisePointer(
  WidgetTester tester,
  DragFixture game,
  PointerDeviceKind kind,
) async {
  final source = rack(0);
  final size = tester.getSize(source);
  final grabOffset = Offset(-size.width * .27, size.height * .21);
  final grabWithinTile = size.center(Offset.zero) + grabOffset;
  final target = tester.getCenter(cell(119));
  final pointer = await beginDrag(
    tester,
    source,
    kind: kind,
    grabOffset: grabOffset,
  );
  expect(feedback, findsOneWidget);
  expect(tester.getSize(feedback), size);
  expect(
    find.descendant(of: feedback, matching: find.text('A')),
    findsOneWidget,
  );
  await pointer.moveTo(target);
  await tester.pump();
  expect(
    (tester.getRect(feedback).topLeft + grabWithinTile - target).distance,
    lessThan(1),
    reason: 'The original off-centre grab point must stay under the pointer.',
  );
  expect(find.byKey(const ValueKey('drop-highlight-119')), findsOneWidget);
  await pointer.up();
  await tester.pump();
  expect(feedback, findsNothing);
  expect(game.commands.last, {
    'op': 'place',
    'rack': 0,
    'row': 7,
    'col': 7,
    'letter': 'A',
  });
}

Future<void> exerciseStagedAndRejectedDrops(
  WidgetTester tester,
  DragFixture game,
  PointerDeviceKind kind,
) async {
  final board = game.state!['board'] as List;
  ((game.state!['rack'] as List)[0] as Map)['placed'] = true;
  board[119] = {
    'letter': 'A',
    'blank': false,
    'pending': true,
    'rack': 0,
    'owner': 0,
    'score': 1,
  };
  board[118] = {'letter': 'C', 'pending': false, 'owner': 0, 'score': 3};
  game.changed();
  await tester.pump();

  final stagedSize = tester.getSize(cell(119));
  final stagedGrab = Offset(stagedSize.width * .2, -stagedSize.height * .2);
  final pointer = await beginDrag(
    tester,
    cell(119),
    kind: kind,
    grabOffset: stagedGrab,
  );
  final target = tester.getCenter(cell(120));
  await pointer.moveTo(target);
  await tester.pump();
  expect(feedback, findsOneWidget);
  expect(tester.getSize(feedback), stagedSize);
  expect(
    (tester.getCenter(feedback) + stagedGrab - target).distance,
    lessThan(1),
  );
  await pointer.up();
  await tester.pump();
  expect(game.commands.last, {
    'op': 'place',
    'rack': 0,
    'row': 7,
    'col': 8,
    'letter': 'A',
  });

  final acceptedCount = game.commands.length;
  final occupied = await beginDrag(tester, rack(1), kind: kind);
  await occupied.moveTo(tester.getCenter(cell(118)));
  await tester.pump();
  expect(find.byKey(const ValueKey('drop-highlight-118')), findsNothing);
  await occupied.up();
  await tester.pump();
  expect(game.commands, hasLength(acceptedCount));
  expect(board[118]['letter'], 'C');

  final outside = await beginDrag(tester, rack(1), kind: kind);
  await outside.moveTo(const Offset(8, 8));
  await tester.pump();
  await outside.up();
  await tester.pump();
  expect(feedback, findsNothing);
  expect(game.commands, hasLength(acceptedCount));

  final cancelled = await beginDrag(tester, rack(1), kind: kind);
  await cancelled.moveTo(tester.getCenter(cell(120)));
  await tester.pump();
  expect(feedback, findsOneWidget);
  await cancelled.cancel();
  await tester.pump();
  expect(feedback, findsNothing);
  expect(game.commands, hasLength(acceptedCount));
  expect((game.state!['rack'] as List)[1]['letter'], 'B');
}

void dragContractTests(TargetPlatform? platform) {
  final environment = platform?.name ?? 'browser defaults';
  for (final viewport in viewports.entries) {
    testWidgets(
      '$environment ${viewport.key}: touch, mouse and stylus keep grab position, map drops and reject cancelled or occupied moves',
      (tester) async {
        final game = DragFixture();
        await mountDrag(tester, game, size: viewport.value);
        if (platform != null) {
          expect(
            Theme.of(tester.element(find.byType(GamePage))).platform,
            platform,
          );
        }
        expectVisible(tester, rack(0), viewport.value);
        expectVisible(tester, cell(119), viewport.value);
        for (final kind in pointers) {
          await exercisePointer(tester, game, kind);
        }
        expect(game.commands, hasLength(pointers.length));
        await exerciseStagedAndRejectedDrops(
          tester,
          game,
          platform == TargetPlatform.android || platform == TargetPlatform.iOS
              ? PointerDeviceKind.touch
              : PointerDeviceKind.mouse,
        );
        expect(game.commands, hasLength(pointers.length + 1));
        expect(tester.takeException(), isNull);
      },
      variant: platform == null
          ? const DefaultTestVariant()
          : TargetPlatformVariant.only(platform),
    );
  }
}

void main() {
  setUp(() => SharedPreferences.setMockInitialValues({}));
  for (final platform in platforms) {
    dragContractTests(platform);
  }
  if (kIsWeb) dragContractTests(null);
}
