import 'dart:ui' show PointerDeviceKind;

import 'package:flutter/material.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:jot_flutter/main.dart';

import 'interactive_fixture.dart';
import 'multiplayer_fixture.dart' show room;

class DragFixture extends InteractiveFixture {
  int networkRevision = 10;
  String activeRoom = room;
  DragFixture() {
    state!['revision'] = 7;
    state!['rack'] = [
      {'letter': 'A', 'blank': false, 'placed': false, 'score': 1},
      {'letter': 'B', 'blank': false, 'placed': false, 'score': 3},
      {'letter': '@', 'blank': true, 'placed': false, 'score': 0},
    ];
  }
  @override
  int get onlineRevision => networkRevision;
  @override
  String? get roomId => online ? activeRoom : null;
}

Finder rack(int index) => find.byKey(ValueKey('rack-tile-$index'));
Finder cell(int index) => find.byKey(ValueKey('board-cell-$index'));
final feedback = find.byKey(const ValueKey('drag-tile-feedback'));

Future<void> mountDrag(
  WidgetTester tester,
  DragFixture game, {
  Size size = const Size(1440, 1000),
}) async {
  tester.view.physicalSize = size;
  tester.view.devicePixelRatio = 1;
  addTearDown(tester.view.resetPhysicalSize);
  addTearDown(tester.view.resetDevicePixelRatio);
  await tester.pumpWidget(
    MaterialApp(home: GamePage(controller: game, initialize: false)),
  );
  await tester.pump();
}

Future<TestGesture> beginDrag(
  WidgetTester tester,
  Finder source, {
  PointerDeviceKind kind = PointerDeviceKind.touch,
  Offset grabOffset = Offset.zero,
}) async {
  final pointer = await tester.startGesture(
    tester.getCenter(source) + grabOffset,
    kind: kind,
  );
  await pointer.moveBy(const Offset(25, -25));
  await tester.pump();
  return pointer;
}

Future<void> dragTo(
  WidgetTester tester,
  Finder source,
  Offset destination,
) async {
  final pointer = await beginDrag(tester, source);
  await pointer.moveTo(destination);
  await tester.pump();
  await pointer.up();
  await tester.pump();
}
