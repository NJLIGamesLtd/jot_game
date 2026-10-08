import 'dart:convert';

import 'package:flutter/services.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:jot_core/jot_core.dart';

void main() {
  TestWidgetsFlutterBinding.ensureInitialized();

  late String dictionary;
  setUpAll(() async {
    dictionary = await rootBundle.loadString('assets/dictionary/words.txt');
    expect(
      dictionary.split('\n').where((word) => word.isNotEmpty).length,
      143722,
    );
  });

  Future<Map<String, dynamic>> command(
    CoreTransport engine,
    Map<String, dynamic> value,
  ) async {
    return jsonDecode(await engine.execute(jsonEncode(value)))
        as Map<String, dynamic>;
  }

  test(
    'native isolate crosses C ABI for complete local turns and persistence',
    () async {
      final engine = await createCore(dictionary, seed: 12345);
      final restored = await createCore(dictionary, seed: 98765, robot: -1);
      addTearDown(engine.close);
      addTearDown(restored.close);

      var response = await command(engine, {'op': 'state'});
      final initial = response['state'] as Map<String, dynamic>;
      expect(response['ok'], isTrue);
      expect(initial['board'], hasLength(256));
      expect(initial['rack'], isEmpty);
      expect(initial['dictionaryWords'], 143722);
      expect(initial['canDraw'], isTrue);
      expect(initial['canHint'], isFalse);
      expect(initial['canPlay'], isFalse);

      // Malformed native commands return errors without advancing the RNG,
      // revision, turn, rack, or bags.
      final malformed = jsonDecode(await engine.execute('{broken')) as Map;
      expect(malformed['ok'], isFalse);
      expect(malformed['state'], equals(initial));
      response = await command(engine, {
        'op': 'new_game',
        'seed': -1,
        'robot': 1,
      });
      expect(response['ok'], isFalse);
      expect(response['state'], equals(initial));
      response = await command(engine, {
        'op': 'place',
        'rack': 0,
        'row': 7.0,
        'col': 7,
        'letter': 'A',
      });
      expect(response['ok'], isFalse);
      expect(response['state'], equals(initial));

      // Concurrent callers retain FIFO execution and copied response buffers.
      final draws = await Future.wait([
        command(engine, {'op': 'draw', 'bag': 'vowel'}),
        command(engine, {'op': 'draw', 'bag': 'consonant'}),
      ]);
      expect(draws[0]['ok'], isTrue);
      expect(draws[1]['ok'], isTrue);
      expect((draws[0]['state'] as Map)['rack'], hasLength(1));
      expect((draws[1]['state'] as Map)['rack'], hasLength(2));
      response = await command(engine, {'op': 'fill'});
      expect(response['ok'], isTrue);
      expect((response['state'] as Map)['rack'], hasLength(8));
      expect((response['state'] as Map)['canHint'], isTrue);
      expect((response['state'] as Map)['canDraw'], isFalse);

      final beforeHint = response['state'];
      response = await command(engine, {'op': 'hint'});
      expect(response['ok'], isTrue, reason: response['message'] as String);
      expect(response['state'], equals(beforeHint));
      expect((response['hint'] as Map)['placements'], isNotEmpty);
      response = await command(engine, {'op': 'hint', 'apply': true});
      expect(response['ok'], isTrue);
      final pendingState = response['state'] as Map;
      expect((pendingState['preview'] as Map)['valid'], isTrue);
      expect(pendingState['canPlay'], isTrue);
      expect(pendingState['canDraw'], isFalse);
      final pendingTiles = (pendingState['board'] as List).where(
        (tile) => tile is Map && tile['pending'] == true,
      );
      expect(pendingTiles, isNotEmpty);
      expect(
        pendingTiles.every((tile) => (tile as Map)['rack'] is int),
        isTrue,
      );

      final saveResponse = await command(engine, {'op': 'save'});
      final save = saveResponse['save'];
      expect((save as Map)['version'], 1);
      response = await command(restored, {'op': 'restore', 'save': save});
      expect(response['ok'], isTrue);
      expect(response['state'], equals(pendingState));
      response = await command(restored, {'op': 'save'});
      expect(response['save'], equals(save));
      final corrupted = jsonDecode(jsonEncode(save)) as Map<String, dynamic>;
      (corrupted['fullState'] as Map)['rngState'] = 0;
      response = await command(restored, {'op': 'restore', 'save': corrupted});
      expect(response['ok'], isFalse);
      expect(response['state'], equals(pendingState));

      final score = (pendingState['preview'] as Map)['score'] as int;
      response = await command(engine, {'op': 'play'});
      expect(response['ok'], isTrue);
      var state = response['state'] as Map;
      expect((state['scores'] as List)[0], score);
      expect(state['currentPlayer'], 1);
      response = await command(engine, {'op': 'robot'});
      expect(response['ok'], isTrue, reason: response['message'] as String);
      state = response['state'] as Map;
      expect(state['currentPlayer'], 0);
      expect((state['scores'] as List)[1], greaterThan(0));
      expect((response['hint'] as Map)['placements'], isNotEmpty);
      final committed = await command(engine, {'op': 'save'});
      response = await command(restored, {
        'op': 'restore',
        'save': committed['save'],
      });
      expect(response['ok'], isTrue);
      expect(response['state'], equals(state));

      // Closing joins all prior requests and concurrent close calls share the
      // same completion, after the C++ handle has been destroyed.
      final finalState = command(engine, {'op': 'state'});
      final closing = engine.close();
      final closingAgain = engine.close();
      expect(identical(closing, closingAgain), isTrue);
      expect((await finalState)['ok'], isTrue);
      await closing;
      await expectLater(engine.execute('{"op":"state"}'), throwsStateError);
    },
    timeout: const Timeout(Duration(minutes: 2)),
  );

  test(
    'transport validates creation and rejects embedded C string terminators',
    () async {
      await expectLater(createCore(dictionary, seed: -1), throwsArgumentError);
      await expectLater(
        createCore(dictionary, seed: 0x100000000),
        throwsArgumentError,
      );
      await expectLater(createCore(dictionary, robot: 4), throwsArgumentError);
      await expectLater(createCore('AT\u0000CAT'), throwsArgumentError);
      await expectLater(createCore(''), throwsStateError);
      final engine = await createCore(dictionary, seed: 0, robot: -1);
      addTearDown(engine.close);
      final before = await command(engine, {'op': 'state'});
      await expectLater(
        engine.execute('{"op":"pass"}\u0000trailing'),
        throwsArgumentError,
      );
      expect(await command(engine, {'op': 'state'}), equals(before));
      await engine.close();
      await engine.close();
    },
  );
}
