import 'multiplayer_fixture.dart' show room, inviteUrl;
import 'presentation_fixture.dart';

class InteractiveFixture extends PresentationFixture {
  final commands = <Map<String, dynamic>>[];
  bool online = false;
  bool joining = false;
  bool hasInvite = false;
  bool resumable = false;
  bool roomJoined = true;
  int initialized = 0;
  int created = 0;
  int retries = 0;
  int returns = 0;
  String? joinedLink;
  String? resumedRoom;
  Future<void> Function(String)? action;
  @override
  bool get isOnline => online;
  @override
  bool get joiningOnline => joining;
  @override
  int? get yourPlayer => online ? 0 : null;
  @override
  bool get joined => roomJoined;
  @override
  String? get inviteUrl => hasInvite ? jotInvite : null;
  String get jotInvite => inviteUrlFixture;
  @override
  bool get canResumeOnline => resumable;
  @override
  List<Map<String, dynamic>> get savedOnlineGames => resumable
      ? [
          {'roomId': room},
        ]
      : [];
  @override
  Future<void> initialize() async {
    initialized++;
  }

  @override
  Future<void> createOnline() async {
    created++;
  }

  @override
  Future<void> joinOnline(String link) async {
    joinedLink = link;
  }

  @override
  Future<void> retryOnline() async {
    retries++;
  }

  @override
  Future<void> resumeOnline([String? wantedRoom]) async {
    resumedRoom = wantedRoom ?? 'latest';
  }

  @override
  Future<void> returnToLocal() async {
    returns++;
    online = false;
    joining = false;
  }

  @override
  Future<void> act(String op, [Map<String, dynamic> fields = const {}]) async {
    commands.add({'op': op, ...fields});
    await action?.call(op);
  }

  void changed() => notifyListeners();
}

final inviteUrlFixture = inviteUrl;
