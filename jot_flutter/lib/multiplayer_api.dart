import 'dart:convert';
import 'dart:math';

import 'package:flutter/foundation.dart';
import 'package:http/http.dart' as http;

/// Resolve an explicit deployment override or the web app's own origin. Native
/// apps use the public deployment because they have no browser origin.
Uri multiplayerApiBase({
  String configured = const String.fromEnvironment('JOT_API_BASE'),
  bool web = kIsWeb,
  Uri? currentUri,
}) => Uri.parse(
  configured.isNotEmpty
      ? configured
      : web
      ? (currentUri ?? Uri.base).origin
      : 'https://jot-game.pages.dev',
);

/// HTTP transports commands and snapshots; the server's C++ engine owns rules.
abstract interface class MultiplayerApi {
  Future<Map<String, dynamic>> create(String requestId);
  Future<Map<String, dynamic>> join(
    String roomId,
    String inviteToken,
    String requestId,
  );
  Future<Map<String, dynamic>> read(String roomId, String playerToken);
  Future<Map<String, dynamic>> act(
    String roomId,
    String playerToken,
    Map<String, dynamic> request,
  );
  void close();
}

class MultiplayerException implements Exception {
  final String message;
  final String? code;
  final Map<String, dynamic>? snapshot;
  final bool retryable;
  const MultiplayerException(
    this.message, {
    this.code,
    this.snapshot,
    this.retryable = false,
  });
  @override
  String toString() => message;
}

class HttpMultiplayerApi implements MultiplayerApi {
  final http.Client _client;
  final Uri base;
  HttpMultiplayerApi({http.Client? client, Uri? base})
    : _client = client ?? http.Client(),
      base = base ?? multiplayerApiBase();

  Future<Map<String, dynamic>> _request(
    String method,
    String path, {
    String? playerToken,
    Map<String, dynamic>? body,
  }) async {
    final request = http.Request(method, base.resolve(path));
    request.headers['Content-Type'] = 'application/json';
    if (playerToken != null) {
      request.headers['Authorization'] = 'Bearer $playerToken';
    }
    if (body != null) request.body = jsonEncode(body);
    try {
      final response = await (() async => http.Response.fromStream(
        await _client.send(request),
      ))().timeout(const Duration(seconds: 30));
      final decoded = jsonDecode(response.body);
      if (decoded is! Map<String, dynamic>) {
        throw const FormatException('Expected a game snapshot.');
      }
      if (response.statusCode >= 200 && response.statusCode < 300) {
        return decoded;
      }
      throw MultiplayerException(
        decoded['error'] as String? ??
            'The game server could not complete this request.',
        code: decoded['code'] as String?,
        snapshot: decoded['state'] is Map ? decoded : null,
        retryable: response.statusCode >= 500 || response.statusCode == 429,
      );
    } on MultiplayerException {
      rethrow;
    } catch (_) {
      // URLs, bearer tokens and transport diagnostics must never appear in UI.
      throw const MultiplayerException(
        'Connection interrupted. Reconnecting will safely retry your request.',
        retryable: true,
      );
    }
  }

  @override
  Future<Map<String, dynamic>> create(String requestId) =>
      _request('POST', '/api/rooms', body: {'requestId': requestId});

  @override
  Future<Map<String, dynamic>> join(
    String roomId,
    String inviteToken,
    String requestId,
  ) => _request(
    'POST',
    '/api/rooms/$roomId/join',
    body: {'inviteToken': inviteToken, 'requestId': requestId},
  );

  @override
  Future<Map<String, dynamic>> read(String roomId, String playerToken) =>
      _request('GET', '/api/rooms/$roomId', playerToken: playerToken);

  @override
  Future<Map<String, dynamic>> act(
    String roomId,
    String playerToken,
    Map<String, dynamic> request,
  ) => _request(
    'POST',
    '/api/rooms/$roomId/actions',
    playerToken: playerToken,
    body: request,
  );

  @override
  void close() => _client.close();
}

/// A random request identity is persisted before sending a mutating request.
/// Retrying uses the same identity, including after a browser reload.
String newRequestId() {
  final random = Random.secure();
  final bytes = List<int>.generate(16, (_) => random.nextInt(256));
  bytes[6] = (bytes[6] & 0x0f) | 0x40;
  bytes[8] = (bytes[8] & 0x3f) | 0x80;
  final hex = bytes.map((b) => b.toRadixString(16).padLeft(2, '0')).join();
  return '${hex.substring(0, 8)}-${hex.substring(8, 12)}-'
      '${hex.substring(12, 16)}-${hex.substring(16, 20)}-${hex.substring(20)}';
}

class GameInvitation {
  final String roomId;
  final String inviteToken;
  const GameInvitation(this.roomId, this.inviteToken);

  static GameInvitation? parse(String link) {
    try {
      final uri = Uri.parse(link.trim());
      final fragment = Uri.splitQueryString(uri.fragment);
      final room = uri.queryParameters['room'] ?? fragment['join'];
      final invite = fragment['invite'];
      if (room == null ||
          invite == null ||
          !RegExp(r'^[a-f0-9]{32}$').hasMatch(room) ||
          !RegExp(r'^[a-f0-9]{64}$').hasMatch(invite)) {
        return null;
      }
      return GameInvitation(room, invite);
    } catch (_) {
      return null;
    }
  }
}
