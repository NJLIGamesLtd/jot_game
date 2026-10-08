import 'package:flutter/services.dart';
import 'multiplayer_api.dart';

/// Capture the invitation before calling this. Removing it prevents a later
/// reload from overriding an explicit switch back to a local game.
Future<void> clearInvitationLocation(Uri current) async {
  if (GameInvitation.parse(current.toString()) == null) return;
  final query = Map<String, String>.from(current.queryParameters)
    ..remove('room');
  final clean = current.replace(
    query: Uri(queryParameters: query).query,
    fragment: '',
  );
  await SystemNavigator.routeInformationUpdated(uri: clean, replace: true);
}
