# Online games

Open https://jot-game.pages.dev/, choose **Online game**, and create a game.
Copy the invitation and send it to your opponent. They open the link and join
as the second player. Both players can then take turns on separate devices.
No account is required. The computer and same-device modes are still available.

The invitation grants the second seat to the first person who joins. The
invitation does not contain the creator's player credential. After joining,
the app remembers each player's session on that device. Keep that browser's
site data to resume a game; this version does not provide account-based recovery
or transfer of a claimed seat to another device.

## Authority and privacy

A Cloudflare Worker routes each room to its own SQLite-backed Durable Object.
The server executes the same C++ engine as the Flutter app, compiled to
WebAssembly. Clients send intentions; they cannot submit scores or replace
the saved board. A full save, containing both racks and random state, stays
on the server. The C++ player view exposes the shared board and the requesting
player's rack. Draft tiles and hint results are private to the active player.

Only the authenticated player whose turn it is can draw, stage, recall, shuffle,
request a hint, play, or pass. Both players must join before play begins.
Each action includes a room revision and a unique request ID. The server checks
the revision and remembers completed requests so retries cannot apply a move
twice. Another tab's stale action receives a conflict and refreshes its view.

The Flutter client polls for updates while the online game is open and resumes
from server state after a reload or reconnection. Polling returns the same
private view used in action responses. Games expire after 30 days of inactivity.

## Deployment

`multiplayer/wrangler.jsonc` configures the `jot-multiplayer` Worker and its room
binding. The Pages configuration binds that Worker as `MULTIPLAYER`.
`web/_worker.js` forwards `/api/*` through this service binding; the Flutter
assets remain static. Both the app and API use the public Pages origin.

See [DEPLOYMENT.md](DEPLOYMENT.md) for build, test, and deployment commands.
