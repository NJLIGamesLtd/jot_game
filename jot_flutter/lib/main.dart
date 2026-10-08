import 'dart:async';
import 'package:flutter/material.dart';
import 'package:flutter/services.dart';
import 'game_controller.dart';
import 'browser_location.dart';

const ink = Color(0xff183c38);
const paper = Color(0xfff6f2e8);
const copper = Color(0xffb45b35);
Future<void> main() async {
  WidgetsFlutterBinding.ensureInitialized();
  final initialUri = Uri.base;
  await clearInvitationLocation(initialUri);
  runApp(JotApp(initialUri: initialUri));
}

class JotApp extends StatelessWidget {
  final Uri? initialUri;
  const JotApp({super.key, this.initialUri});
  @override
  Widget build(BuildContext context) => MaterialApp(
    title: 'Jot',
    debugShowCheckedModeBanner: false,
    initialRoute: '/',
    theme: ThemeData(
      useMaterial3: true,
      colorScheme: ColorScheme.fromSeed(seedColor: ink, surface: paper),
      scaffoldBackgroundColor: paper,
      appBarTheme: const AppBarTheme(
        backgroundColor: paper,
        foregroundColor: ink,
      ),
      filledButtonTheme: FilledButtonThemeData(
        style: FilledButton.styleFrom(
          backgroundColor: ink,
          foregroundColor: Colors.white,
          padding: const EdgeInsets.symmetric(horizontal: 20, vertical: 16),
        ),
      ),
    ),
    home: GamePage(initialUri: initialUri),
  );
}

/// A drag identifies a tile and the game snapshot that offered it.
/// C++ still validates and owns every accepted placement.
class RackTileDrag {
  final int rackIndex;
  final String letter;
  final bool blank;
  final Object? revision;
  final String? roomId;
  final int onlineRevision;
  final int player;

  const RackTileDrag({
    required this.rackIndex,
    required this.letter,
    required this.blank,
    required this.revision,
    required this.roomId,
    required this.onlineRevision,
    required this.player,
  });
}

class GamePage extends StatefulWidget {
  final GameController? controller;
  final bool initialize;
  final Uri? initialUri;
  const GamePage({
    super.key,
    this.controller,
    this.initialize = true,
    this.initialUri,
  });
  @override
  State<GamePage> createState() => _GamePageState();
}

class _GamePageState extends State<GamePage> {
  late final GameController game;
  int? selected;
  @override
  void initState() {
    super.initState();
    game = widget.controller ?? GameController(initialUri: widget.initialUri);
    if (widget.initialize) {
      unawaited(game.initialize());
    }
  }

  @override
  void dispose() {
    game.dispose();
    super.dispose();
  }

  bool get canAct => game.canAct;

  RackTileDrag dragForRack(int rackIndex, {String? assignedLetter}) {
    final state = game.state!;
    final tile = (state['rack'] as List)[rackIndex] as Map;
    return RackTileDrag(
      rackIndex: rackIndex,
      letter: assignedLetter ?? tile['letter'] as String,
      blank: tile['blank'] == true,
      revision: state['revision'],
      roomId: game.roomId,
      onlineRevision: game.onlineRevision,
      player: state['currentPlayer'] as int,
    );
  }

  bool canDrop(RackTileDrag drag) {
    if (!canAct ||
        drag.roomId != game.roomId ||
        drag.onlineRevision != game.onlineRevision) {
      return false;
    }
    final state = game.state!;
    final rack = state['rack'] as List;
    if (drag.revision != state['revision'] ||
        drag.player != state['currentPlayer'] ||
        drag.rackIndex < 0 ||
        drag.rackIndex >= rack.length) {
      return false;
    }
    final tile = rack[drag.rackIndex] as Map;
    return drag.blank == (tile['blank'] == true) &&
        (drag.blank || drag.letter == tile['letter']);
  }

  Future<void> dropAt(RackTileDrag drag, int index) async {
    if (!canDrop(drag) || (game.state!['board'] as List)[index] != null) {
      return;
    }
    await placeAt(index, drag: drag);
  }

  Future<void> placeAt(int index, {RackTileDrag? drag}) async {
    if (!canAct) return;
    final tile = (game.state!['board'] as List)[index];
    if (tile is Map && tile['pending'] == true) {
      final rack = game.state!['rack'] as List;
      // A pending cell is removed by its rack index, supplied in snapshots.
      final rackIndex = tile['rack'];
      if (rackIndex is int) {
        await game.act('unplace', {'rack': rackIndex});
      } else {
        await game.act('recall');
      }
      if (!mounted) return;
      setState(() {
        if (selected != null && selected! >= rack.length) selected = null;
      });
      return;
    }
    final rackIndex = drag?.rackIndex ?? selected;
    if (rackIndex == null || tile != null) return;
    final rack = game.state!['rack'] as List;
    if (rackIndex >= rack.length) {
      setState(() => selected = null);
      return;
    }
    final rackTile = rack[rackIndex] as Map;
    var letter = drag?.letter ?? rackTile['letter'] as String;
    if (rackTile['blank'] == true && (drag == null || letter == '@')) {
      final choice = await showDialog<String>(
        context: context,
        builder: (context) => AlertDialog(
          title: const Text('Choose a letter for your blank'),
          content: SizedBox(
            width: 320,
            child: Wrap(
              spacing: 6,
              runSpacing: 6,
              children: List.generate(26, (i) {
                final letter = String.fromCharCode(65 + i);
                return SizedBox(
                  width: 42,
                  height: 42,
                  child: OutlinedButton(
                    style: OutlinedButton.styleFrom(padding: EdgeInsets.zero),
                    onPressed: () => Navigator.pop(context, letter),
                    child: Text(letter),
                  ),
                );
              }),
            ),
          ),
        ),
      );
      if (choice == null || !mounted) return;
      letter = choice;
    }
    if (drag != null && !canDrop(drag)) return;
    await game.act('place', {
      'rack': rackIndex,
      'row': index ~/ 16,
      'col': index % 16,
      'letter': letter,
    });
    if (mounted) setState(() => selected = null);
  }

  Future<void> newGame() async {
    final choice = await showDialog<String>(
      context: context,
      builder: (context) => AlertDialog(
        title: const Text('Start a new game'),
        content: SizedBox(
          width: 350,
          child: SingleChildScrollView(
            child: Column(
              mainAxisSize: MainAxisSize.min,
              crossAxisAlignment: CrossAxisAlignment.stretch,
              children: [
                const Text(
                  'Play with a friend on separate devices, or start a local game. Your online games stay saved on this device.',
                ),
                const SizedBox(height: 16),
                FilledButton.icon(
                  onPressed: () => Navigator.pop(context, 'online'),
                  icon: const Icon(Icons.public),
                  label: const Text('Online game'),
                ),
                const SizedBox(height: 8),
                OutlinedButton.icon(
                  onPressed: () => Navigator.pop(context, 'join'),
                  icon: const Icon(Icons.link),
                  label: const Text('Join with invite link'),
                ),
                for (final session in game.savedOnlineGames) ...[
                  const SizedBox(height: 8),
                  TextButton(
                    onPressed: () =>
                        Navigator.pop(context, 'resume:${session['roomId']}'),
                    child: Text(
                      'Resume online game · ${(session['roomId'] as String).substring(0, 8)}',
                    ),
                  ),
                ],
                if (game.isOnline || game.joiningOnline) ...[
                  const SizedBox(height: 8),
                  TextButton(
                    onPressed: () => Navigator.pop(context, 'local'),
                    child: const Text('Return to saved local game'),
                  ),
                ],
                const Divider(height: 28),
                const Text(
                  'Starting a new local game replaces the previous local game.',
                  style: TextStyle(fontSize: 12),
                ),
                const SizedBox(height: 8),
                OutlinedButton(
                  onPressed: () => Navigator.pop(context, 'computer'),
                  child: const Text('Play computer'),
                ),
                TextButton(
                  onPressed: () => Navigator.pop(context, 'same-device'),
                  child: const Text('Two players on this device'),
                ),
              ],
            ),
          ),
        ),
        actions: [
          TextButton(
            onPressed: () => Navigator.pop(context),
            child: const Text('Cancel'),
          ),
        ],
      ),
    );
    if (choice == null || !mounted) {
      return;
    }
    selected = null;
    if (choice == 'online') {
      await game.createOnline();
      return;
    }
    if (choice == 'join') {
      await joinGame();
      return;
    }
    if (choice.startsWith('resume:')) {
      await game.resumeOnline(choice.substring(7));
      return;
    }
    if (game.isOnline || game.joiningOnline || game.state == null) {
      await game.returnToLocal();
    }
    if (choice == 'local') return;
    await game.act('new_game', {
      'seed': DateTime.now().millisecondsSinceEpoch & 0xffffffff,
      'robot': choice == 'computer' ? 1 : -1,
    });
  }

  Future<void> joinGame() async {
    final input = TextEditingController();
    final link = await showDialog<String>(
      context: context,
      builder: (context) => AlertDialog(
        title: const Text('Join your friend'),
        content: SizedBox(
          width: 360,
          child: TextField(
            controller: input,
            autofocus: true,
            maxLines: 3,
            decoration: const InputDecoration(
              labelText: 'Paste the full invite link',
              hintText: 'https://jot-game.pages.dev/?room=…#invite=…',
            ),
          ),
        ),
        actions: [
          TextButton(
            onPressed: () => Navigator.pop(context),
            child: const Text('Cancel'),
          ),
          FilledButton(
            onPressed: () => Navigator.pop(context, input.text),
            child: const Text('Join game'),
          ),
        ],
      ),
    );
    // A route may still animate while its text field disposes.
    await Future<void>.delayed(const Duration(milliseconds: 300));
    input.dispose();
    if (link != null && mounted) {
      selected = null;
      await game.joinOnline(link);
    }
  }

  Future<void> copyInvite() async {
    final invite = game.inviteUrl;
    if (invite == null) return;
    try {
      await Clipboard.setData(ClipboardData(text: invite));
      if (mounted) {
        ScaffoldMessenger.of(context).showSnackBar(
          const SnackBar(
            content: Text('Invite link copied. Send it to your friend.'),
          ),
        );
      }
    } catch (_) {
      if (mounted) {
        ScaffoldMessenger.of(context).showSnackBar(
          const SnackBar(
            content: Text('Select and copy the invite link shown below.'),
          ),
        );
      }
    }
  }

  void showRules() {
    showDialog<void>(
      context: context,
      builder: (context) => AlertDialog(
        title: const Text('The chess of crosswords'),
        content: const SingleChildScrollView(
          child: Text(
            'Draw eight tiles, choosing vowels and consonants. Touch a tile, drag it onto the board, and release to place it. Drag a staged tile to move it, or tap it to take it back. You can also select a rack tile and tap an empty square.\n\n'
            'Your first word must cover a centre square. Later plays must join the board. Place tiles in one continuous row or column, reusing existing letters. Every word formed must be in the dictionary.\n\n'
            'Letter and word bonuses apply to newly placed tiles. Playing all eight tiles earns 100 extra points. Blank tiles score zero.\n\n'
            'Hint suggests a legal move; Place hint stages it for review. Play commits your move. Two consecutive passes end the game. Remaining tile points are deducted at the end.\n\n'
            'Online game creates an invite link for a friend on another device. Open or paste the full link to join. Each player sees their own rack. Games update automatically and reconnect safely. Keep using the same browser or device to retain your seat.\n\n'
            'Pinch or scroll to zoom the board. Local games are saved on this device. Online rooms are saved on the server; your private player access stays on this device.',
          ),
        ),
        actions: [
          TextButton(
            onPressed: () => Navigator.pop(context),
            child: const Text('Got it'),
          ),
        ],
      ),
    );
  }

  @override
  Widget build(BuildContext context) => AnimatedBuilder(
    animation: game,
    builder: (context, _) {
      final state = game.state;
      return Scaffold(
        appBar: AppBar(
          title: const Row(
            children: [
              Text(
                'jot',
                style: TextStyle(
                  fontSize: 36,
                  fontWeight: FontWeight.w900,
                  letterSpacing: -2,
                ),
              ),
              SizedBox(width: 16),
              Flexible(
                child: Text(
                  'THE CHESS OF CROSSWORDS',
                  style: TextStyle(fontSize: 10, letterSpacing: 1.5),
                ),
              ),
            ],
          ),
          actions: [
            IconButton(
              onPressed: showRules,
              tooltip: 'How to play',
              icon: const Icon(Icons.help_outline),
            ),
            TextButton(
              onPressed: game.busy ? null : newGame,
              child: Text(game.isOnline ? 'Games' : 'New game'),
            ),
            const SizedBox(width: 12),
          ],
        ),
        body: state == null
            ? Center(
                child: Padding(
                  padding: const EdgeInsets.all(32),
                  child: Column(
                    mainAxisSize: MainAxisSize.min,
                    children: [
                      if (!game.failed) const CircularProgressIndicator(),
                      const SizedBox(height: 20),
                      Text(game.message, textAlign: TextAlign.center),
                      if (game.failed)
                        TextButton(
                          onPressed: () => game.isOnline || game.joiningOnline
                              ? game.retryOnline()
                              : game.initialize(),
                          child: const Text('Retry'),
                        ),
                    ],
                  ),
                ),
              )
            : LayoutBuilder(
                builder: (context, box) {
                  final wide = box.maxWidth >= 760;
                  final controls = controlPanel(state);
                  final board = JotBoard(
                    state: state,
                    onTap: canAct ? placeAt : null,
                    canDrop: canDrop,
                    onDrop: dropAt,
                    dragForRack: (rackIndex, letter) =>
                        dragForRack(rackIndex, assignedLetter: letter),
                    canDrag: canAct,
                  );
                  if (wide) {
                    return Center(
                      child: ConstrainedBox(
                        constraints: const BoxConstraints(maxWidth: 1260),
                        child: Padding(
                          padding: const EdgeInsets.all(24),
                          child: Row(
                            crossAxisAlignment: CrossAxisAlignment.start,
                            children: [
                              Expanded(child: board),
                              const SizedBox(width: 28),
                              SizedBox(
                                width: 300,
                                child: SingleChildScrollView(child: controls),
                              ),
                            ],
                          ),
                        ),
                      ),
                    );
                  }
                  return SingleChildScrollView(
                    child: Padding(
                      padding: const EdgeInsets.fromLTRB(12, 4, 12, 24),
                      child: Column(
                        children: [
                          scoreStrip(state),
                          const SizedBox(height: 12),
                          board,
                          const SizedBox(height: 16),
                          controlPanel(state, withScores: false),
                        ],
                      ),
                    ),
                  );
                },
              ),
      );
    },
  );

  Widget scoreStrip(Map<String, dynamic> state) {
    final current = state['currentPlayer'] as int;
    final scores = state['scores'] as List;
    return Row(
      children: List.generate(
        2,
        (player) => Expanded(
          child: Container(
            margin: EdgeInsets.only(left: player == 1 ? 8 : 0),
            padding: const EdgeInsets.all(14),
            decoration: BoxDecoration(
              color: current == player ? ink : Colors.white,
              borderRadius: BorderRadius.circular(14),
            ),
            child: Column(
              crossAxisAlignment: CrossAxisAlignment.start,
              children: [
                Text(
                  player == state['robotPlayer']
                      ? 'Computer'
                      : game.isOnline
                      ? player == game.yourPlayer
                            ? 'You'
                            : 'Friend'
                      : 'Player ${player + 1}',
                  style: TextStyle(
                    color: current == player ? Colors.white : ink,
                  ),
                ),
                const SizedBox(height: 4),
                Text(
                  '${scores[player]}',
                  style: TextStyle(
                    fontSize: 26,
                    fontWeight: FontWeight.w800,
                    color: current == player ? Colors.white : ink,
                  ),
                ),
              ],
            ),
          ),
        ),
      ),
    );
  }

  Widget controlPanel(Map<String, dynamic> state, {bool withScores = true}) {
    final rack = state['rack'] as List;
    final preview = state['preview'] as Map;
    final current = state['currentPlayer'] as int;
    final remaining =
        (state['remaining'] as List)[game.yourPlayer ?? current] as Map;
    final over = state['over'] == true;
    if (selected != null && selected! >= rack.length) selected = null;
    if (game.isOnline && (!game.joined || current != game.yourPlayer)) {
      selected = null;
    }
    return Column(
      crossAxisAlignment: CrossAxisAlignment.stretch,
      children: [
        if (withScores) ...[scoreStrip(state), const SizedBox(height: 24)],
        Text(
          'YOUR RACK  ·  ${rack.length}/8  ·  DRAG TO BOARD',
          style: const TextStyle(
            color: ink,
            fontSize: 11,
            fontWeight: FontWeight.w700,
            letterSpacing: 1.2,
          ),
        ),
        const SizedBox(height: 10),
        SizedBox(
          height: 52,
          child: Row(
            children: List.generate(
              8,
              (i) => Expanded(
                child: Padding(
                  padding: const EdgeInsets.symmetric(horizontal: 2),
                  child: i >= rack.length
                      ? Container(
                          decoration: BoxDecoration(
                            borderRadius: BorderRadius.circular(8),
                            border: Border.all(
                              color: ink.withValues(alpha: .15),
                            ),
                          ),
                        )
                      : LayoutBuilder(
                          builder: (context, constraints) {
                            final tile = rack[i] as Map;
                            final face = RackTileFace(
                              key: ValueKey('rack-tile-$i'),
                              tile: tile,
                              selected: selected == i,
                              onTap: canAct
                                  ? () => setState(
                                      () => selected = selected == i ? null : i,
                                    )
                                  : null,
                            );
                            return Semantics(
                              button: true,
                              excludeSemantics: true,
                              selected: selected == i,
                              label: 'Rack tile ${i + 1}, ${tile['letter']}',
                              hint: 'Drag onto the board, or tap to select',
                              onTap: canAct
                                  ? () => setState(
                                      () => selected = selected == i ? null : i,
                                    )
                                  : null,
                              child: Draggable<RackTileDrag>(
                                data: dragForRack(i),
                                maxSimultaneousDrags: canAct ? 1 : 0,
                                rootOverlay: true,
                                onDragStarted: () =>
                                    setState(() => selected = null),
                                feedback: SizedBox(
                                  key: const ValueKey('drag-tile-feedback'),
                                  width: constraints.maxWidth,
                                  height: constraints.maxHeight,
                                  child: RackTileFace(
                                    tile: tile,
                                    selected: true,
                                  ),
                                ),
                                childWhenDragging: Opacity(
                                  opacity: .25,
                                  child: face,
                                ),
                                child: face,
                              ),
                            );
                          },
                        ),
                ),
              ),
            ),
          ),
        ),
        const SizedBox(height: 12),
        Wrap(
          spacing: 8,
          runSpacing: 8,
          children: [
            OutlinedButton(
              onPressed:
                  canAct && state['canDraw'] == true && remaining['vowels'] > 0
                  ? () => game.act('draw', {'bag': 'vowel'})
                  : null,
              child: Text('Vowel · ${remaining['vowels']}'),
            ),
            OutlinedButton(
              onPressed:
                  canAct &&
                      state['canDraw'] == true &&
                      remaining['consonants'] > 0
                  ? () => game.act('draw', {'bag': 'consonant'})
                  : null,
              child: Text('Consonant · ${remaining['consonants']}'),
            ),
            TextButton(
              onPressed: canAct && state['canDraw'] == true
                  ? () => game.act('fill')
                  : null,
              child: const Text('Fill rack'),
            ),
          ],
        ),
        const SizedBox(height: 20),
        if (game.isOnline || game.joiningOnline) ...[
          Container(
            padding: const EdgeInsets.all(14),
            decoration: BoxDecoration(
              color: const Color(0xffe4ece5),
              borderRadius: BorderRadius.circular(14),
            ),
            child: Column(
              crossAxisAlignment: CrossAxisAlignment.stretch,
              children: [
                Row(
                  children: [
                    Icon(
                      game.connected
                          ? Icons.cloud_done_outlined
                          : Icons.cloud_off_outlined,
                      size: 18,
                      color: ink,
                    ),
                    const SizedBox(width: 8),
                    Expanded(
                      child: Text(
                        game.joiningOnline
                            ? 'Connecting to online game'
                            : 'Online · You are player ${game.yourPlayer! + 1}',
                        style: const TextStyle(
                          color: ink,
                          fontWeight: FontWeight.w600,
                          fontSize: 12,
                        ),
                      ),
                    ),
                  ],
                ),
                if (game.isOnline && !game.joined) ...[
                  const SizedBox(height: 8),
                  const Text(
                    'Waiting for your friend to join.',
                    style: TextStyle(fontSize: 12),
                  ),
                ],
                if (game.inviteUrl != null && !game.joiningOnline) ...[
                  const SizedBox(height: 8),
                  OutlinedButton.icon(
                    onPressed: copyInvite,
                    icon: const Icon(Icons.copy, size: 16),
                    label: const Text('Copy invite link'),
                  ),
                  SelectableText(
                    game.inviteUrl!,
                    maxLines: 2,
                    style: const TextStyle(fontSize: 11, color: ink),
                  ),
                ],
                if (game.connectionError != null) ...[
                  const SizedBox(height: 8),
                  Text(
                    game.connectionError!,
                    style: const TextStyle(fontSize: 12, color: copper),
                  ),
                  TextButton(
                    onPressed: game.busy ? null : game.retryOnline,
                    child: const Text('Reconnect'),
                  ),
                ],
              ],
            ),
          ),
          const SizedBox(height: 18),
        ] else ...[
          OutlinedButton.icon(
            onPressed: game.busy ? null : game.createOnline,
            icon: const Icon(Icons.public, size: 18),
            label: const Text('Online game'),
          ),
          TextButton(
            onPressed: game.busy ? null : joinGame,
            child: const Text('Join with invite link'),
          ),
          if (game.canResumeOnline)
            TextButton(
              onPressed: game.busy ? null : game.resumeOnline,
              child: const Text('Resume online game'),
            ),
          const SizedBox(height: 16),
        ],
        Text(
          over
              ? 'Game complete'
              : game.isOnline && (!game.joined || current != game.yourPlayer)
              ? 'Waiting for your friend'
              : 'Your next word',
          style: const TextStyle(
            color: ink,
            fontSize: 28,
            fontWeight: FontWeight.w800,
          ),
        ),
        const SizedBox(height: 8),
        Row(
          children: [
            if (game.busy) ...[
              const SizedBox(
                width: 16,
                height: 16,
                child: CircularProgressIndicator(strokeWidth: 2),
              ),
              const SizedBox(width: 10),
            ],
            Expanded(
              child: Text(
                game.message,
                style: const TextStyle(color: ink, height: 1.45),
              ),
            ),
          ],
        ),
        if (game.storageWarning != null) ...[
          const SizedBox(height: 8),
          Text(
            game.storageWarning!,
            style: const TextStyle(color: copper, fontSize: 12),
          ),
        ],
        if (!over && current == state['robotPlayer'] && !game.busy)
          OutlinedButton(
            onPressed: () => game.act('robot'),
            child: const Text('Retry computer turn'),
          ),
        const SizedBox(height: 22),
        Container(
          padding: const EdgeInsets.all(16),
          decoration: BoxDecoration(
            color: Colors.white,
            borderRadius: BorderRadius.circular(14),
          ),
          child: Row(
            children: [
              Expanded(
                child: Text(
                  preview['valid'] == true
                      ? ((preview['words'] as List)
                            .map((w) => (w as Map)['word'])
                            .join(' · '))
                      : preview['message'] as String? ?? 'Place your tiles',
                  style: const TextStyle(color: ink),
                ),
              ),
              const SizedBox(width: 8),
              Text(
                '${preview['score'] ?? 0}',
                style: const TextStyle(
                  color: copper,
                  fontSize: 30,
                  fontWeight: FontWeight.w800,
                ),
              ),
            ],
          ),
        ),
        const SizedBox(height: 12),
        FilledButton.icon(
          onPressed: canAct && state['canPlay'] == true
              ? () {
                  selected = null;
                  game.act('play');
                }
              : null,
          icon: const Icon(Icons.check),
          label: const Text('Play word'),
        ),
        const SizedBox(height: 10),
        Wrap(
          spacing: 8,
          runSpacing: 4,
          children: [
            TextButton.icon(
              onPressed: canAct
                  ? () {
                      selected = null;
                      game.act('recall');
                    }
                  : null,
              icon: const Icon(Icons.undo, size: 18),
              label: const Text('Recall'),
            ),
            TextButton.icon(
              onPressed: canAct && state['canShuffle'] == true
                  ? () {
                      selected = null;
                      game.act('shuffle');
                    }
                  : null,
              icon: const Icon(Icons.shuffle, size: 18),
              label: const Text('Shuffle'),
            ),
            TextButton.icon(
              onPressed: canAct && state['canHint'] == true
                  ? () => game.act('hint')
                  : null,
              icon: const Icon(Icons.lightbulb_outline, size: 18),
              label: const Text('Hint'),
            ),
            TextButton(
              onPressed: canAct
                  ? () {
                      selected = null;
                      game.act('pass');
                    }
                  : null,
              child: const Text('Pass'),
            ),
          ],
        ),
        if (game.hint != null) ...[
          const SizedBox(height: 12),
          Text(
            'Suggestion: ${game.hint!['word']} · ${game.hint!['score']} points',
            style: const TextStyle(color: ink, fontWeight: FontWeight.w600),
          ),
          OutlinedButton(
            onPressed: canAct
                ? () {
                    selected = null;
                    game.act('hint', {'apply': true});
                  }
                : null,
            child: const Text('Place hint'),
          ),
        ],
        const SizedBox(height: 20),
        const Text(
          'L2 / L3  letter bonus     W2 / W3  word bonus',
          style: TextStyle(fontSize: 11, color: ink),
        ),
      ],
    );
  }
}

class RackTileFace extends StatelessWidget {
  final Map tile;
  final bool selected;
  final VoidCallback? onTap;
  const RackTileFace({
    super.key,
    required this.tile,
    this.selected = false,
    this.onTap,
  });

  @override
  Widget build(BuildContext context) => Material(
    color: selected
        ? copper
        : tile['placed'] == true
        ? const Color(0xffdedbd1)
        : Colors.white,
    elevation: selected ? 4 : 0,
    borderRadius: BorderRadius.circular(8),
    child: InkWell(
      borderRadius: BorderRadius.circular(8),
      onTap: onTap,
      child: Stack(
        children: [
          Center(
            child: Text(
              tile['blank'] == true && tile['letter'] == '@'
                  ? '◇'
                  : tile['letter'] as String,
              style: TextStyle(
                fontSize: 23,
                fontWeight: FontWeight.w800,
                color: selected ? Colors.white : ink,
              ),
            ),
          ),
          Positioned(
            right: 3,
            bottom: 3,
            child: Text(
              '${tile['score'] ?? 0}',
              style: TextStyle(
                fontSize: 8,
                color: selected ? Colors.white : ink,
              ),
            ),
          ),
        ],
      ),
    ),
  );
}

class JotBoard extends StatelessWidget {
  final Map<String, dynamic> state;
  final void Function(int)? onTap;
  final bool Function(RackTileDrag)? canDrop;
  final void Function(RackTileDrag, int)? onDrop;
  final RackTileDrag Function(int, String)? dragForRack;
  final bool canDrag;
  const JotBoard({
    super.key,
    required this.state,
    this.onTap,
    this.canDrop,
    this.onDrop,
    this.dragForRack,
    this.canDrag = false,
  });
  @override
  Widget build(BuildContext context) {
    final board = state['board'] as List;
    final bonuses = state['bonuses'] as List;
    return AspectRatio(
      aspectRatio: 1,
      child: ClipRRect(
        borderRadius: BorderRadius.circular(14),
        child: InteractiveViewer(
          minScale: 1,
          maxScale: 3,
          child: Container(
            color: ink,
            padding: const EdgeInsets.all(5),
            child: GridView.builder(
              padding: EdgeInsets.zero,
              physics: const NeverScrollableScrollPhysics(),
              itemCount: 256,
              gridDelegate: const SliverGridDelegateWithFixedCrossAxisCount(
                crossAxisCount: 16,
                crossAxisSpacing: 1,
                mainAxisSpacing: 1,
              ),
              itemBuilder: (context, i) {
                final tile = board[i] as Map?;
                final bonus = bonuses[i] as String;
                final colour = tile != null
                    ? (tile['pending'] == true
                          ? const Color(0xffeac29b)
                          : tile['owner'] == 0
                          ? const Color(0xfffaf7ed)
                          : const Color(0xffc8ddd5))
                    : switch (bonus) {
                        'L2' => const Color(0xffe5dbaa),
                        'L3' => const Color(0xffb9d2d2),
                        'W2' => const Color(0xffbdd3ae),
                        'W3' => const Color(0xffc6cde4),
                        _ => const Color(0xffe9e9de),
                      };
                final label = tile != null ? tile['letter'] as String : bonus;
                Widget cell = Semantics(
                  key: ValueKey('board-cell-$i'),
                  button: onTap != null,
                  excludeSemantics: true,
                  onTap: onTap == null ? null : () => onTap!(i),
                  label:
                      'Row ${i ~/ 16 + 1}, column ${i % 16 + 1}, '
                      '${label.isEmpty ? 'empty' : label}',
                  child: Material(
                    color: colour,
                    child: InkWell(
                      onTap: onTap == null ? null : () => onTap!(i),
                      child: Stack(
                        children: [
                          Center(
                            child: FittedBox(
                              child: Padding(
                                padding: const EdgeInsets.all(2),
                                child: Text(
                                  label,
                                  style: TextStyle(
                                    color: ink,
                                    fontSize: tile != null ? 19 : 9,
                                    fontWeight: tile != null
                                        ? FontWeight.w800
                                        : FontWeight.w600,
                                  ),
                                ),
                              ),
                            ),
                          ),
                          if (tile != null)
                            Positioned(
                              right: 2,
                              bottom: 0,
                              child: Text(
                                '${tile['score'] ?? 0}',
                                style: const TextStyle(fontSize: 7, color: ink),
                              ),
                            ),
                        ],
                      ),
                    ),
                  ),
                );
                if (tile != null &&
                    tile['pending'] == true &&
                    tile['rack'] is int &&
                    dragForRack != null) {
                  final stagedContents = cell;
                  cell = LayoutBuilder(
                    builder: (context, constraints) => Draggable<RackTileDrag>(
                      data: dragForRack!(
                        tile['rack'] as int,
                        tile['letter'] as String,
                      ),
                      maxSimultaneousDrags: canDrag ? 1 : 0,
                      rootOverlay: true,
                      feedback: SizedBox(
                        key: const ValueKey('drag-tile-feedback'),
                        width: constraints.maxWidth,
                        height: constraints.maxHeight,
                        child: RackTileFace(tile: tile, selected: true),
                      ),
                      childWhenDragging: Opacity(
                        opacity: .25,
                        child: stagedContents,
                      ),
                      child: stagedContents,
                    ),
                  );
                }
                final contents = cell;
                return DragTarget<RackTileDrag>(
                  onWillAcceptWithDetails: (details) =>
                      tile == null && canDrop?.call(details.data) == true,
                  onAcceptWithDetails: (details) =>
                      onDrop?.call(details.data, i),
                  builder: (context, candidates, rejected) => Stack(
                    fit: StackFit.expand,
                    children: [
                      contents,
                      if (candidates.isNotEmpty)
                        IgnorePointer(
                          child: Container(
                            key: ValueKey('drop-highlight-$i'),
                            decoration: BoxDecoration(
                              color: copper.withValues(alpha: .2),
                              border: Border.all(color: copper, width: 2),
                            ),
                          ),
                        ),
                    ],
                  ),
                );
              },
            ),
          ),
        ),
      ),
    );
  }
}
