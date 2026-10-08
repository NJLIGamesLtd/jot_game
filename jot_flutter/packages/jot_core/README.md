# jot_core

Portable C++17 Jot rules and AI, exposed through three C ABI functions:

- `jot_create(dictionary_utf8, seed_uint32, robot_player)` returns an owned handle.
- `jot_execute(handle, command_json)` returns a borrowed UTF-8 response.
- `jot_destroy(handle)` frees the handle; null is accepted.

`robot_player` is -1 for local two-player mode, or 0/1 for the computer. A caller
must copy a response before executing another command or destroying the handle.
One worker owns each handle. Independent handles may run concurrently.

Commands use an `op` field: `state`, `new_game`, `draw`, `fill`, `place`, `unplace`,
`recall`, `shuffle`, `play`, `pass`, `hint`, `robot`, `save`, and `restore`.
All responses contain `ok`, `message`, and the current public `state`. `save`
includes a versioned `save` object; `restore` accepts that object. `hint` can
include a candidate and accepts `apply: true` to stage it. Coordinates and rack
indices are zero-based. Invalid commands preserve the game state.

Example commands:

```json
{"op":"draw","bag":"vowel"}
{"op":"place","rack":0,"row":7,"col":7,"letter":"A"}
{"op":"hint","apply":true}
```

The Dart entry point conditionally exports native and web worker transports.
The build hook uses Flutter's code assets mechanism on mobile/desktop. The web
application must load `jot_bridge.js` and compile the C++ sources with Emscripten;
see the parent app's README. CMake provides an independent native build and tests.
