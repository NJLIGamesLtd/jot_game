import 'dart:async';
import 'dart:convert';
import 'package:flutter/foundation.dart';
import 'package:flutter/services.dart';
import 'package:jot_core/jot_core.dart';
import 'package:shared_preferences/shared_preferences.dart';
import 'multiplayer_api.dart';

/// Presentation and storage adapter. Rules, validation, search and saved-game
/// serialization are performed exclusively by the C++ core.
class GameController extends ChangeNotifier {
  static const saveKey = 'jot.localGame.v1';
  static const onlineSaveKey = 'jot.onlineGame.v1';
  static const onlineSetupKey = 'jot.onlineSetup.v1';
  static const localModeKey = 'jot.preferLocal.v1';
  static const onlineHistoryKey = 'jot.onlineRooms.v1';
  static const invitationHistoryKey = 'jot.onlineInvitations.v1';
  final MultiplayerApi _api;
  final Uri? _initialUri;
  final Duration pollInterval;
  final Future<SharedPreferences> Function() _preferencesLoader;
  final Future<String> Function()? _dictionaryLoader;
  final Future<CoreTransport> Function(String)? _coreFactory;
  GameController({
    MultiplayerApi? multiplayerApi,
    Uri? initialUri,
    this.pollInterval = const Duration(seconds: 2),
    Future<SharedPreferences> Function()? preferencesLoader,
    Future<String> Function()? dictionaryLoader,
    Future<CoreTransport> Function(String)? coreFactory,
  }) : _api = multiplayerApi ?? HttpMultiplayerApi(),
       _initialUri = initialUri,
       _preferencesLoader = preferencesLoader ?? SharedPreferences.getInstance,
       _dictionaryLoader = dictionaryLoader,
       _coreFactory = coreFactory;
  CoreTransport? _core;
  SharedPreferences? _preferences;
  Map<String, dynamic>? _session;
  Map<String, dynamic>? _pendingAction;
  Map<String, dynamic>? _setup;
  Timer? _pollTimer;
  final Set<int> _requests = {};
  final Map<int, Completer<void>> _requestDone = {};
  Future<void> _storageQueue = Future<void>.value();
  bool get _requestInFlight => _requests.contains(_generation);
  int _generation = 0;
  bool connected = false;
  bool _automaticReconnect = true;
  String? connectionError;
  bool get isOnline => _session != null;
  bool get joiningOnline => _setup != null;
  int? get yourPlayer => _session?['yourPlayer'] as int?;
  String? get inviteUrl => _session?['inviteUrl'] as String?;
  String? get roomId => _session?['roomId'] as String?;
  bool get joined => _session?['joined'] == true;
  bool get canResumeOnline =>
      !isOnline && _preferences?.getString(onlineSaveKey) != null;
  List<Map<String, dynamic>> get savedOnlineGames {
    final history = _readStored(onlineHistoryKey) ?? {};
    return history.values
        .whereType<Map>()
        .map(Map<String, dynamic>.from)
        .where(_validSession)
        .toList()
        .reversed
        .toList();
  }

  int get onlineRevision => _session?['revision'] as int? ?? -1;
  bool get canAct =>
      !busy &&
      !failed &&
      !joiningOnline &&
      state != null &&
      state!['over'] != true &&
      (isOnline
          ? connected &&
                joined &&
                _pendingAction == null &&
                state!['currentPlayer'] == yourPlayer
          : state!['currentPlayer'] != state!['robotPlayer']);
  Map<String, dynamic>? state;
  Map<String, dynamic>? hint;
  String message = 'Loading your game…';
  String? storageWarning;
  bool busy = true;
  bool failed = false;
  bool _disposed = false;

  Future<void> initialize() async {
    final generation = ++_generation;
    _pollTimer?.cancel();
    busy = true;
    failed = false;
    storageWarning = null;
    state = null;
    hint = null;
    message = 'Loading your game…';
    _notify();
    try {
      _preferences = await _preferencesLoader();
      if (!_active(generation)) return;
      _session = _readStored(onlineSaveKey);
      if (!_validSession(_session)) _session = null;
      _pendingAction = _session?['pendingAction'] is Map
          ? Map<String, dynamic>.from(_session!['pendingAction'] as Map)
          : null;
      _setup = _readStored(onlineSetupKey);
      final invitation = GameInvitation.parse(
        (_initialUri ?? Uri.base).toString(),
      );
      if (invitation != null) {
        final existing = _savedSession(invitation.roomId);
        if (existing != null) {
          _session = existing;
          _pendingAction = existing['pendingAction'] is Map
              ? Map<String, dynamic>.from(existing['pendingAction'] as Map)
              : null;
          _setup = null;
          await _preferences!.remove(onlineSetupKey);
          await _preferences!.setBool(localModeKey, false);
        }
      }
      if (invitation == null &&
          _setup == null &&
          _preferences!.getBool(localModeKey) == true) {
        _session = null;
        _pendingAction = null;
      }
      if (invitation != null && invitation.roomId != roomId) {
        if (_setup?['roomId'] != invitation.roomId ||
            _setup?['inviteToken'] != invitation.inviteToken) {
          final previous =
              (_readStored(invitationHistoryKey) ?? {})[invitation.roomId];
          _setup =
              previous is Map &&
                  previous['inviteToken'] == invitation.inviteToken
              ? Map<String, dynamic>.from(previous)
              : {
                  'operation': 'join',
                  'roomId': invitation.roomId,
                  'inviteToken': invitation.inviteToken,
                  'requestId': newRequestId(),
                };
          await _storeSetup();
        }
      }
      if (_setup != null) {
        await _finishSetup(generation);
      } else if (_session != null) {
        await _onlineRequest(generation);
        _startPolling();
      } else {
        await _loadLocal(generation);
      }
    } catch (error) {
      if (_active(generation)) {
        message = 'Unable to start Jot: $error';
        failed = true;
      }
    } finally {
      if (_active(generation)) {
        busy = false;
        _notify();
      }
    }
  }

  Future<void> _loadLocal(int generation) async {
    await _core?.close().catchError((Object _) {});
    _core = null;
    final dictionary =
        await (_dictionaryLoader?.call() ??
            rootBundle.loadString('assets/dictionary/words.txt'));
    if (!_active(generation)) return;
    final core =
        await (_coreFactory?.call(dictionary) ??
            createCore(
              dictionary,
              seed: DateTime.now().millisecondsSinceEpoch & 0xffffffff,
            ));
    if (!_active(generation)) {
      await core.close();
      return;
    }
    _core = core;
    final saved = _preferences!.getString(saveKey);
    if (saved != null) {
      try {
        final result = await _send({
          'op': 'restore',
          'save': jsonDecode(saved),
        });
        if (result['ok'] != true) {
          message = 'Saved game could not be restored. Start a new game.';
        }
      } catch (_) {
        await _send({'op': 'state'});
        message = 'Saved game could not be restored.';
      }
    } else {
      await _send({'op': 'state'});
      message = 'Choose your tiles to begin.';
    }
    await _computerTurn();
    if (state != null) {
      await _save();
    }
  }

  Future<Map<String, dynamic>> _send(Map<String, dynamic> command) async {
    final result =
        jsonDecode(await _core!.execute(jsonEncode(command)))
            as Map<String, dynamic>;
    if (_disposed) {
      return result;
    }
    if (result['state'] is Map) {
      state = Map<String, dynamic>.from(result['state'] as Map);
    }
    hint = command['op'] == 'hint' && result['hint'] is Map
        ? Map<String, dynamic>.from(result['hint'] as Map)
        : null;
    message = result['message'] as String? ?? '';
    _notify();
    return result;
  }

  Future<void> act(String op, [Map<String, dynamic> fields = const {}]) async {
    if (isOnline) {
      if (!canAct || _disposed) return;
      busy = true;
      _pendingAction = {
        'revision': onlineRevision,
        'requestId': newRequestId(),
        'command': {'op': op, ...fields},
      };
      _notify();
      final generation = _generation;
      try {
        if (await _saveSession()) {
          await _onlineRequest(generation);
        } else {
          _pendingAction = null;
          message =
              'The move was not sent because it could not be saved. Retry after enabling browser storage.';
        }
      } finally {
        if (_active(generation)) {
          busy = false;
          _notify();
        }
      }
      return;
    }
    if (joiningOnline) return;
    if (busy || _core == null || _disposed) return;
    busy = true;
    _notify();
    try {
      final result = await _send({'op': op, ...fields});
      if (result['ok'] == true) {
        await _save();
        await _computerTurn();
        await _save();
      }
    } catch (error) {
      message = 'The action could not finish: $error';
    } finally {
      busy = false;
      _notify();
    }
  }

  Future<void> _computerTurn() async {
    if (state == null ||
        state!['over'] == true ||
        state!['currentPlayer'] != state!['robotPlayer']) {
      return;
    }
    message = 'The computer is thinking…';
    _notify();
    await _send({'op': 'robot'});
    message = 'Computer: $message';
    _notify();
  }

  Future<void> _save() async {
    // Saving must not replace the presentation message or hint.
    final response =
        jsonDecode(await _core!.execute('{"op":"save"}'))
            as Map<String, dynamic>;
    if (response['ok'] == true && response['save'] != null) {
      try {
        final saved = await _preferences!.setString(
          saveKey,
          jsonEncode(response['save']),
        );
        storageWarning = saved
            ? null
            : 'This game could not be saved. Keep the app open to continue.';
      } catch (_) {
        storageWarning =
            'This game could not be saved. Keep the app open to continue.';
      }
    } else {
      storageWarning =
          'This game could not be saved. Keep the app open to continue.';
    }
  }

  void _notify() {
    if (!_disposed) notifyListeners();
  }

  @override
  void dispose() {
    _disposed = true;
    ++_generation;
    _pollTimer?.cancel();
    _api.close();
    unawaited(_core?.close().catchError((Object _) {}));
    super.dispose();
  }

  bool _active(int generation) => !_disposed && generation == _generation;

  Map<String, dynamic>? _readStored(String key) {
    try {
      final value = _preferences!.getString(key);
      return value == null
          ? null
          : Map<String, dynamic>.from(jsonDecode(value) as Map);
    } catch (_) {
      return null;
    }
  }

  bool _validSession(Map<String, dynamic>? session) =>
      session != null &&
      session['roomId'] is String &&
      session['playerToken'] is String &&
      (session['yourPlayer'] == 0 || session['yourPlayer'] == 1) &&
      session['revision'] is int;

  Map<String, dynamic>? _savedSession(String wantedRoom) {
    final latest = _readStored(onlineSaveKey);
    if (_validSession(latest) && latest!['roomId'] == wantedRoom) return latest;
    final history = _readStored(onlineHistoryKey);
    final found = history?[wantedRoom];
    if (found is! Map) return null;
    final session = Map<String, dynamic>.from(found);
    return _validSession(session) ? session : null;
  }

  Future<bool> _saveSession() async {
    if (_session == null) return false;
    final generation = _generation;
    final savedSession = {..._session!, 'pendingAction': _pendingAction};
    try {
      final completion = Completer<bool>();
      _storageQueue = _storageQueue.then((_) async {
        try {
          if (!_active(generation)) {
            completion.complete(false);
            return;
          }
          final saved = await _preferences!.setString(
            onlineSaveKey,
            jsonEncode(savedSession),
          );
          if (saved) {
            final history = _readStored(onlineHistoryKey) ?? {};
            history[savedSession['roomId'] as String] = savedSession;
            await _preferences!.setString(
              onlineHistoryKey,
              jsonEncode(history),
            );
          }
          completion.complete(saved);
        } catch (error, stack) {
          completion.completeError(error, stack);
        }
      });
      final saved = await completion.future;
      if (_active(generation)) {
        storageWarning = saved
            ? null
            : 'Online play could not be saved on this device.';
      }
      return saved;
    } catch (_) {
      if (_active(generation)) {
        storageWarning = 'Online play could not be saved on this device.';
      }
      return false;
    }
  }

  Future<void> _storeSetup() async {
    _preferences ??= await _preferencesLoader();
    if (!await _preferences!.setString(onlineSetupKey, jsonEncode(_setup))) {
      throw const MultiplayerException(
        'Enable browser storage to create or join an online game.',
      );
    }
    if (_setup?['operation'] == 'join') {
      final history = _readStored(invitationHistoryKey) ?? {};
      history[_setup!['roomId'] as String] = _setup;
      await _preferences!.setString(invitationHistoryKey, jsonEncode(history));
    }
  }

  Future<void> createOnline() => _setup?['operation'] == 'create'
      ? retryOnline()
      : _beginSetup({'operation': 'create', 'requestId': newRequestId()});

  Future<void> joinOnline(String link) async {
    final invitation = GameInvitation.parse(link);
    if (invitation == null) {
      message = 'Paste the full Jot invite link, including the part after #.';
      _notify();
      return;
    }
    if (roomId == invitation.roomId && isOnline) {
      await retryOnline();
      return;
    }
    if (_setup?['operation'] == 'join' &&
        _setup?['roomId'] == invitation.roomId &&
        _setup?['inviteToken'] == invitation.inviteToken) {
      await retryOnline();
      return;
    }
    if (_savedSession(invitation.roomId) != null) {
      await resumeOnline(invitation.roomId);
      return;
    }
    final previous =
        (_readStored(invitationHistoryKey) ?? {})[invitation.roomId];
    if (previous is Map && previous['inviteToken'] == invitation.inviteToken) {
      await _beginSetup(Map<String, dynamic>.from(previous));
      return;
    }
    await _beginSetup({
      'operation': 'join',
      'roomId': invitation.roomId,
      'inviteToken': invitation.inviteToken,
      'requestId': newRequestId(),
    });
  }

  Future<void> _beginSetup(Map<String, dynamic> setup) async {
    if (busy || _disposed) return;
    final generation = ++_generation;
    _pollTimer?.cancel();
    _setup = setup;
    busy = true;
    failed = false;
    connectionError = null;
    message = setup['operation'] == 'create'
        ? 'Creating an online game…'
        : 'Joining your friend…';
    _notify();
    try {
      await _storeSetup();
      await _finishSetup(generation);
    } catch (error) {
      if (_active(generation)) _networkFailure(error);
    } finally {
      if (_active(generation)) {
        busy = false;
        _notify();
      }
    }
  }

  Future<void> _finishSetup(int generation) async {
    if (!_active(generation) || _requestInFlight || _setup == null) return;
    _requests.add(generation);
    final setup = _setup!;
    try {
      final response = setup['operation'] == 'create'
          ? await _api.create(setup['requestId'] as String)
          : await _api.join(
              setup['roomId'] as String,
              setup['inviteToken'] as String,
              setup['requestId'] as String,
            );
      if (!_active(generation)) return;
      if (!_validSession(response) || response['state'] is! Map) {
        throw const MultiplayerException(
          'The server returned an incomplete game session.',
          retryable: true,
        );
      }
      _session = {
        'roomId': response['roomId'],
        'playerToken': response['playerToken'],
        'yourPlayer': response['yourPlayer'],
        'revision': response['revision'],
        'joined': response['joined'],
        'inviteUrl': response['inviteUrl'],
      };
      _pendingAction = null;
      _setup = null;
      _acceptOnline(response);
      final saved = await _saveSession();
      if (!_active(generation)) return;
      if (saved) await _preferences!.remove(onlineSetupKey);
      await _preferences!.setBool(localModeKey, false);
      _startPolling();
    } catch (error) {
      if (_active(generation)) _networkFailure(error);
    } finally {
      _requests.remove(generation);
      if (_active(generation)) _notify();
    }
  }

  void _acceptOnline(Map<String, dynamic> response, {bool action = false}) {
    if (response['state'] is! Map || response['revision'] is! int) {
      throw const MultiplayerException(
        'The server returned an incomplete game snapshot.',
        retryable: true,
      );
    }
    final revision = response['revision'] as int;
    if (revision < onlineRevision) return;
    final changed = revision != onlineRevision;
    final reconnected = !connected;
    _session!['revision'] = revision;
    _session!['joined'] = response['joined'] == true;
    if (response['inviteUrl'] is String) {
      _session!['inviteUrl'] = response['inviteUrl'];
    }
    state = Map<String, dynamic>.from(response['state'] as Map);
    if (action || changed) {
      hint = response['hint'] is Map
          ? Map<String, dynamic>.from(response['hint'] as Map)
          : null;
    }
    connected = true;
    _automaticReconnect = true;
    failed = false;
    connectionError = null;
    if (response['message'] is String && action) {
      message = response['message'] as String;
    } else if (!joined) {
      message =
          'Share your invite link. The game starts when your friend joins.';
    } else if (state!['over'] == true) {
      message = 'Game complete.';
    } else if (state!['currentPlayer'] != yourPlayer) {
      message = 'Waiting for your friend’s turn.';
    } else if (changed ||
        reconnected ||
        message.startsWith('Waiting') ||
        message.startsWith('Share') ||
        message.startsWith('Loading')) {
      message = 'Your turn. Draw tiles or place your next word.';
    }
  }

  void _networkFailure(Object error) {
    connected = false;
    connectionError = error is MultiplayerException
        ? error.message
        : 'Unable to reach the game server.';
    message = connectionError!;
    if (state == null) failed = true;
  }

  Future<void> _onlineRequest(int generation) async {
    if (_requestInFlight) {
      if (_pendingAction == null) return;
      await _requestDone[generation]?.future;
    }
    if (!_active(generation) || _session == null) return;
    _requests.add(generation);
    final requestDone = Completer<void>();
    _requestDone[generation] = requestDone;
    final pending = _pendingAction;
    try {
      final response = pending == null
          ? await _api.read(roomId!, _session!['playerToken'] as String)
          : await _api.act(
              roomId!,
              _session!['playerToken'] as String,
              pending,
            );
      if (!_active(generation)) return;
      _acceptOnline(response, action: pending != null);
      if (pending != null) _pendingAction = null;
      await _saveSession();
    } catch (error) {
      if (!_active(generation)) return;
      if (error is MultiplayerException && error.snapshot != null) {
        _acceptOnline(error.snapshot!, action: true);
        _pendingAction = null;
        message =
            'The game changed on another device. Review the refreshed board and try again.';
        await _saveSession();
      } else {
        // Network failures retain the exact request until an authoritative reply.
        if (error is MultiplayerException && !error.retryable) {
          _automaticReconnect = false;
          _pollTimer?.cancel();
          _pendingAction = null;
          await _saveSession();
        }
        if (!_active(generation)) return;
        _networkFailure(error);
      }
    } finally {
      _requests.remove(generation);
      _requestDone.remove(generation);
      requestDone.complete();
      if (_active(generation)) _notify();
    }
  }

  void _startPolling() {
    _pollTimer?.cancel();
    if (_disposed || !isOnline || !_automaticReconnect) return;
    _pollTimer = Timer.periodic(pollInterval, (_) {
      if (!_disposed && !busy && !_requestInFlight && isOnline) {
        unawaited(_onlineRequest(_generation));
      }
    });
  }

  Future<void> retryOnline() async {
    if (busy || _disposed || _requestInFlight) return;
    busy = true;
    failed = false;
    _notify();
    final generation = _generation;
    try {
      if (_setup != null) {
        await _storeSetup();
        await _finishSetup(generation);
      } else if (isOnline) {
        await _onlineRequest(generation);
        _startPolling();
      }
    } catch (error) {
      if (_active(generation)) _networkFailure(error);
    } finally {
      if (_active(generation)) {
        busy = false;
        _notify();
      }
    }
  }

  Future<void> returnToLocal() async {
    if (busy || _disposed) return;
    final generation = ++_generation;
    _pollTimer?.cancel();
    busy = true;
    _session = null;
    _pendingAction = null;
    _setup = null;
    state = null;
    hint = null;
    failed = false;
    connectionError = null;
    connected = false;
    _notify();
    try {
      _preferences ??= await _preferencesLoader();
      await _preferences!.setBool(localModeKey, true);
      await _preferences!.remove(onlineSetupKey);
      await _loadLocal(generation);
    } catch (error) {
      if (_active(generation)) {
        failed = true;
        message = 'Unable to load your local game: $error';
      }
    } finally {
      if (_active(generation)) {
        busy = false;
        _notify();
      }
    }
  }

  Future<void> resumeOnline([String? wantedRoom]) async {
    if (busy || _disposed) return;
    final session = wantedRoom == null
        ? _readStored(onlineSaveKey)
        : _savedSession(wantedRoom);
    if (!_validSession(session)) {
      message = 'This device has no saved online game.';
      _notify();
      return;
    }
    final generation = ++_generation;
    _pollTimer?.cancel();
    busy = true;
    failed = false;
    state = null;
    hint = null;
    _setup = null;
    _session = session;
    _pendingAction = session!['pendingAction'] is Map
        ? Map<String, dynamic>.from(session['pendingAction'] as Map)
        : null;
    _notify();
    try {
      await _preferences!.setBool(localModeKey, false);
      await _preferences!.remove(onlineSetupKey);
      await _onlineRequest(generation);
      _startPolling();
    } finally {
      if (_active(generation)) {
        busy = false;
        _notify();
      }
    }
  }
}
