import 'dart:convert';
import 'dart:ui' show FrameTiming, PointerDeviceKind;

import 'package:flutter/foundation.dart';
import 'package:flutter/material.dart';
import 'package:flutter/scheduler.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:integration_test/integration_test.dart';
import 'package:jot_core/jot_core.dart';
import 'package:jot_flutter/game_controller.dart';
import 'package:jot_flutter/main.dart';
import 'package:shared_preferences/shared_preferences.dart';

// Run with flutter drive --profile; debug widget tests are not frame benchmarks.
// The real UI, persistent worker and full C++ dictionary are used. Only storage
// is in memory to keep benchmark games separate from a player's saved games.
void main() {
  final binding = IntegrationTestWidgetsFlutterBinding.ensureInitialized();
  testWidgets('profile real local play and continuous tile dragging', (
    tester,
  ) async {
    SharedPreferences.setMockInitialValues({});
    final timings = <FrameTiming>[];
    void collect(List<FrameTiming> batch) {
      // The run is bounded, and the collector also has a hard sample limit.
      timings.addAll(batch.take(10000 - timings.length));
    }

    final commands = <String, List<int>>{};
    var largestResponse = '';
    final startup = Stopwatch()..start();
    final game = GameController(
      coreFactory: (dictionary) async => _MeasuredTransport(
        await createCore(dictionary, seed: 12345),
        (command, response, elapsed) {
          final op = (jsonDecode(command) as Map)['op'] as String;
          (commands[op] ??= []).add(elapsed);
          if (response.length > largestResponse.length) {
            largestResponse = response;
          }
        },
      ),
    );
    await tester.pumpWidget(MaterialApp(home: GamePage(controller: game)));
    Future<void> waitForEngine() async {
      final deadline = Stopwatch()..start();
      while (game.busy && deadline.elapsed < const Duration(seconds: 60)) {
        await Future<void>.delayed(const Duration(milliseconds: 10));
        await tester.pump();
      }
      expect(game.busy, isFalse, reason: game.message);
      expect(game.failed, isFalse, reason: game.message);
    }

    await waitForEngine();
    final startupUs = startup.elapsedMicroseconds;
    expect(await game.act('fill'), isTrue);
    await tester.pumpAndSettle();
    // Flush startup frame batches so the gesture sample is a warm-play sample.
    await Future<void>.delayed(const Duration(seconds: 2));
    SchedulerBinding.instance.addTimingsCallback(collect);
    try {
      for (var repetition = 0; repetition < 6; repetition++) {
        final source = find.byKey(const ValueKey('rack-tile-0'));
        final target = find.byKey(const ValueKey('board-cell-119'));
        await tester.ensureVisible(source);
        await tester.pumpAndSettle();
        final start = tester.getCenter(source);
        final end = tester.getCenter(target);
        final pointer = await tester.startGesture(
          start,
          kind: PointerDeviceKind.touch,
        );
        for (var step = 1; step <= 30; step++) {
          await pointer.moveTo(Offset.lerp(start, end, step / 30)!);
          await tester.pump(const Duration(milliseconds: 16));
        }
        await pointer.up();
        await tester.pump();
        await waitForEngine();
        final placed = (game.state!['board'] as List)[119] as Map?;
        expect(placed, isNotNull, reason: 'Measured drag must reach the board');
        expect(placed!['pending'], isTrue);
        expect(placed['rack'], 0);
        expect(await game.act('recall'), isTrue);
        expect((game.state!['board'] as List)[119], isNull);
        await tester.pumpAndSettle();
      }
      for (var turn = 0; turn < 4; turn++) {
        expect(await game.act('fill'), isTrue);
        expect(await game.act('hint'), isTrue);
        expect(game.hint, isNotNull);
        expect(await game.act('hint', {'apply': true}), isTrue);
        expect(await game.act('play'), isTrue);
        await tester.pumpAndSettle();
      }
      expect(
        (game.state!['scores'] as List).every((s) => (s as int) > 0),
        true,
      );
      await Future<void>.delayed(const Duration(seconds: 2));
    } finally {
      SchedulerBinding.instance.removeTimingsCallback(collect);
    }
    expect(timings, isNotEmpty);
    final codec = <int>[];
    for (var sample = 0; sample < 200; sample++) {
      final watch = Stopwatch()..start();
      jsonDecode(largestResponse);
      codec.add(watch.elapsedMicroseconds);
    }
    Map<String, Object> summary(List<int> microseconds) {
      final sorted = [...microseconds]..sort();
      int percentile(double fraction) =>
          sorted[((sorted.length - 1) * fraction).ceil()];
      return {
        'samples': sorted.length,
        'p50_us': percentile(.5),
        'p95_us': percentile(.95),
        'p99_us': percentile(.99),
        'max_us': sorted.last,
        'over_16667_us': sorted.where((t) => t > 16667).length,
        'over_8333_us': sorted.where((t) => t > 8333).length,
      };
    }

    binding.reportData = {
      'mode': kDebugMode ? 'debug-invalid-for-comparison' : 'profile/release',
      'platform': kIsWeb ? 'web' : defaultTargetPlatform.name,
      'viewport': {
        'width': tester.view.physicalSize.width,
        'height': tester.view.physicalSize.height,
      },
      'device_pixel_ratio': tester.view.devicePixelRatio,
      'storage': 'in-memory test preferences',
      'seed': 12345,
      'startup_us': startupUs,
      'frame_build': summary([
        for (final t in timings) t.buildDuration.inMicroseconds,
      ]),
      'frame_raster': summary([
        for (final t in timings) t.rasterDuration.inMicroseconds,
      ]),
      'frame_total': summary([
        for (final t in timings) t.totalSpan.inMicroseconds,
      ]),
      'largest_response_bytes': utf8.encode(largestResponse).length,
      'ui_json_decode': summary(codec),
      'worker_command_roundtrip': {
        for (final entry in commands.entries) entry.key: summary(entry.value),
      },
    };
    expect(tester.takeException(), isNull);
    await tester.pumpWidget(const SizedBox());
  });
}

class _MeasuredTransport implements CoreTransport {
  final CoreTransport inner;
  final void Function(String, String, int) record;
  _MeasuredTransport(this.inner, this.record);
  @override
  Future<String> execute(String command) async {
    final watch = Stopwatch()..start();
    final response = await inner.execute(command);
    record(command, response, watch.elapsedMicroseconds);
    return response;
  }

  @override
  Future<void> close() => inner.close();
}
