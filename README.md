# Jot

Jot is a crossword-style word game with a 16×16 board, eight-tile racks, a
computer opponent, two-player play on one device, and private online games.
The current application uses **Flutter and Dart for the interface** and a
**shared C++ engine for the rules, dictionary, scoring, and computer player**.

You can try the published game at [jot-game.pages.dev](https://jot-game.pages.dev/).
For development, start in **`jot_flutter/`**. The `jot_android/` directory is the
original Android implementation, and `jot_ios/` is an incomplete original Swift
port; both are references for the rewrite.

## Contents

- [Set up your computer](#set-up-your-computer)
- [Get the code and run the app](#get-the-code-and-run-the-app)
- [Run in a browser](#run-in-a-browser)
- [Try your first move](#try-your-first-move)
- [Flutter basics and everyday development](#flutter-basics-and-everyday-development)
- [Find your way around the code](#find-your-way-around-the-code)
- [Run checks and tests](#run-checks-and-tests)
- [Work on online multiplayer](#work-on-online-multiplayer)
- [Build a release](#build-a-release)
- [Troubleshooting](#troubleshooting)
- [Further reading and licensing](#further-reading-and-licensing)

## Set up your computer

Start with one target: the desktop app for your operating system is a useful
first choice. You only need the tools for the target you choose.

### 1. Install Git and Flutter

Install [Git](https://git-scm.com/downloads) to download and work with the source.
Use **Flutter 3.41.9**, the version pinned in this repository's GitHub Actions
workflows. The existing app documentation records Dart 3.11.5 with that SDK.
**Dart is included with Flutter**; a separate Dart installation is unnecessary.

Download Flutter 3.41.9 for your operating system and CPU from the
[Flutter SDK archive](https://docs.flutter.dev/install/archive), then follow the
[manual installation guide](https://docs.flutter.dev/install/manual). Keep the
SDK outside this repository, in a writable location, and add its `bin` directory
to your `PATH` so a terminal can find `flutter` and `dart`.

For example, if you extracted the SDK to `~/development/flutter` on macOS or
Linux, add this to your shell configuration (`~/.zshrc` or `~/.bashrc`):

```sh
export PATH="$HOME/development/flutter/bin:$PATH"
```

On Windows, add your actual Flutter `bin` directory, such as
`C:\dev\flutter\bin`, to your user's **Path** environment variable. Open a new
terminal after changing `PATH`.

Check that the expected SDK is being used:

```sh
flutter --version
dart --version
flutter doctor -v
```

`flutter doctor` checks installed development tools and explains missing setup.
Resolve the issues for your chosen target; you can leave tools for other targets
uninstalled. Prefer the pinned SDK when following this guide; newer SDKs can
change build and test behavior.

### 2. Install your target's tools

The game contains C++ code, so native targets need a C++ compiler as well as
Flutter. Follow the linked Flutter guides for the installation steps.

| Target | Development computer | Tools and project requirements |
| --- | --- | --- |
| macOS desktop | macOS | Full Xcode, its command-line tools and accepted licenses, plus CocoaPods. The app targets macOS 13 or later. [macOS setup](https://docs.flutter.dev/platform-integration/macos/setup) |
| Windows desktop | Windows | Visual Studio with the **Desktop development with C++** workload, including the Windows SDK and CMake tools. Visual Studio is a separate installation from VS Code. [Windows setup](https://docs.flutter.dev/platform-integration/windows/setup) |
| Linux desktop | Linux | Clang, CMake, Ninja, pkg-config, GTK development libraries, and the C++ standard library development package. [Linux setup](https://docs.flutter.dev/platform-integration/linux/setup) |
| Android | macOS, Windows, or Linux | Android Studio, Android SDK tools, an emulator or device, and the NDK for the C++ engine. This project's Java target and CI toolchain use Java 17; SDK/NDK versions come from the pinned Flutter SDK. [Android setup](https://docs.flutter.dev/platform-integration/android/setup) |
| iOS | macOS | Full Xcode, command-line tools, CocoaPods, and an installed simulator runtime or configured device. The app targets iOS 15 or later. A physical device also requires development signing. [iOS setup](https://docs.flutter.dev/platform-integration/ios/setup) |
| Web | macOS, Windows, or Linux | Google Chrome, Python 3, and Emscripten 4.0.13. See [Run in a browser](#run-in-a-browser) for the additional engine build. |

For example, the Linux desktop packages on Ubuntu can be installed with:

```sh
sudo apt-get update
sudo apt-get install -y clang cmake ninja-build pkg-config libgtk-3-dev libstdc++-12-dev
```

For Android, use Android Studio's **SDK Manager** to install the SDK platform
requested by Flutter, Command-line Tools, Platform-Tools, Build-Tools, and
**NDK (Side by side)**. Install the emulator if you plan to use one. Review and
accept the SDK licenses with:

```sh
flutter doctor --android-licenses
```

### 3. Set up an editor

VS Code with the **Flutter** extension (which also installs Dart support), or
Android Studio with its Flutter plugin, provides completion, debugging, and
Flutter tools. Install the SDK above as well as the editor extension.

After cloning the code below, open the **`jot_flutter` folder** as your editor's project. This puts
`pubspec.yaml`, the app's configuration file, at the project root.

## Get the code and run the app

In a terminal, clone the repository and enter the Flutter app:

```sh
git clone https://github.com/NJLIGamesLtd/jot_game.git
cd jot_game/jot_flutter
flutter pub get
flutter devices
```

If you already have a checkout, open a terminal in its `jot_flutter` directory
and run the last two commands. `flutter pub get` downloads Dart dependencies and
connects the local `packages/jot_core` engine package. The first build also
downloads platform dependencies and can take a while.

**Unless a section says otherwise, all following commands run from
`jot_game/jot_flutter/`, the directory containing `pubspec.yaml`.**

Choose the command for your desktop operating system:

| Target | Run command |
| --- | --- |
| macOS | `flutter run -d macos` |
| Windows | `flutter run -d windows` |
| Linux | `flutter run -d linux` |

For Android or iOS, start an emulator/simulator through Android Studio or Xcode,
or connect a device. On Android hardware, enable Developer options and USB
debugging, and authorize your computer. Then run:

```sh
flutter devices
flutter run -d DEVICE_ID
```

Replace `DEVICE_ID` with the identifier printed by `flutter devices`.

The app should open the game board with a rack, scores, and turn guidance.
The native build hook compiles and bundles the C++17 engine automatically;
there is no separate CMake build or library-copy step for running the app.
Computer and same-device games run locally with the bundled dictionary and
require no accounts, API keys, Firebase setup, or backend server.

## Run in a browser

The browser runs the same C++ engine compiled to **WebAssembly (WASM)**, a binary
format browsers can execute. A fresh checkout needs this extra build before
Flutter can load the game.

Install Python 3 and Chrome, then install and activate **Emscripten 4.0.13**, the
version used by CI. The [official Emscripten setup guide](https://emscripten.org/docs/getting_started/downloads.html)
explains platform prerequisites. For macOS/Linux, install the SDK outside the
repository, for example:

```sh
git clone https://github.com/emscripten-core/emsdk.git "$HOME/emsdk"
"$HOME/emsdk/emsdk" install 4.0.13
"$HOME/emsdk/emsdk" activate 4.0.13
source "$HOME/emsdk/emsdk_env.sh"
em++ --version
```

On Windows, use `emsdk.bat install 4.0.13`, `emsdk.bat activate 4.0.13`, and
`emsdk_env.bat` from the SDK directory in Command Prompt. Run subsequent build
commands in that configured terminal. The examples use `python3`; if your
Windows installation exposes Python 3 as `python`, use that command instead.

Return to `jot_flutter`, then run:

```sh
flutter pub get
python3 tools/build_web_core.py
flutter run -d chrome
```

This generates `web/jot_core.js` and `web/jot_core.wasm`, then serves the app and
opens Chrome. These two generated files are ignored by Git. Rebuild them after
changing the C++ engine, and stop/relaunch the app to load the new engine.
On macOS/Linux, source `emsdk_env.sh` again in each new terminal that builds WASM.
Use an HTTP server such as `flutter run`; opening `web/index.html` directly from
the filesystem will not run the app correctly.

This development server supports the computer and same-device modes. To test
online games locally, use the [local multiplayer setup](#work-on-online-multiplayer),
which also supplies the API server and proxy.

## Try your first move

1. Choose a computer or same-device game and use **Fill rack** to draw eight tiles.
2. Drag tiles onto the board, or select a rack tile and then tap a board square.
   The first word must cross one of the four outlined centre squares. Blank
   tiles ask you to choose a letter.
3. Use **Hint**, then **Place hint**, if you want a legal word to try. Press
   **Play word** to commit the move. **Recall** returns unplayed tiles to the rack.
4. Open **How to play** for the full rules, controls, and scoring.

At the start of each turn, fill the rack before playing unless your bags are
empty. If you staged a word too early, **Return tiles & fill rack** lets you
recover. Games save automatically on the current device; reloading resumes the
saved game. **New game** replaces the current local game. Online seat credentials
also live on the device, so clearing app/browser data can lose access to a seat.

## Flutter basics and everyday development

Flutter builds the interface from **widgets**, Dart objects that describe what
appears on screen and how it responds to input. A widget's `build()` method
describes its current appearance. This app uses a `GameController` to send game
actions to the engine, save progress, and notify widgets when state changes.

Useful files and terms:

| Item | Meaning here |
| --- | --- |
| `lib/main.dart` | App entry point, theme, game screen, board, rack, and dialogs. |
| `pubspec.yaml` | App metadata, Dart dependencies, and bundled assets. Run `flutter pub get` after changing it. |
| `pubspec.lock` | Resolved dependency versions. Keep it committed for reproducible app builds. |
| `assets/` | Files bundled into the app, including the dictionary. |
| `test/` | Automated Dart and Flutter tests, including tests of the real engine. |
| Native runner folders | Platform projects that host Flutter: `android/`, `ios/`, `macos/`, `windows/`, and `linux/`. |
| FFI / C ABI | The interface Dart uses to call the compiled C++ engine on native platforms. |

For a first change, try editing text or a theme colour in `lib/main.dart`, then
save and use the terminal running `flutter run`:

- **`r`**: hot reload; apply supported Dart edits while keeping in-memory state.
- **`R`**: hot restart; rerun Dart initialization. This app can restore its saved game.
- **`q`**: stop the run session.

The [Flutter hot reload guide](https://docs.flutter.dev/tools/hot-reload) explains
which edits require restarting. C++ changes require stopping and rerunning the
native app; web also requires rebuilding WASM. Changes to startup logic may
need a hot restart. Check the run terminal for errors and use your editor's
breakpoints to debug Dart code.

After pulling changes, run `flutter pub get`. If C++ changed, rebuild the web
engine before another browser run. Use `npm ci` again in the relevant directory
when its npm lockfile changes.

## Find your way around the code

```text
jot_game/
├── README.md                    Start here
├── .github/workflows/           Test and release build automation
├── jot_flutter/                 Current app; run commands here
│   ├── lib/                     Dart interface and controllers
│   ├── packages/jot_core/       Shared C++ engine, Dart bindings, build hook
│   ├── assets/dictionary/       Bundled word list and provenance
│   ├── web/                    Browser workers, WASM output, Pages API proxy
│   ├── multiplayer/            Online server, TypeScript, server tests
│   ├── test/                   Flutter unit, widget, and integration tests
│   ├── integration_test/       Optional performance harness
│   ├── tools/                  Build, coverage, and verification scripts
│   └── android/, ios/, …       Flutter platform runners
├── jot_android/                Original Android app and reference assets
└── jot_ios/                    Incomplete original Swift port
```

The main paths through the app are:

```text
Local:   Flutter widgets → GameController → native isolate/FFI or web worker/WASM → C++
Online:  Flutter widgets → GameController → HTTP API → room server → C++ WASM
```

The engine owns legal moves, scores, turns, dictionary lookup, AI, and save
validation. Widgets display snapshots and send actions. An **isolate** (native)
or **web worker** (browser) runs local engine work away from the UI thread.
For online games, a server room owns the authoritative game and returns each
player's private view.

| If you want to change… | Start with… |
| --- | --- |
| Layout, colours, tile gestures, dialogs | [`lib/main.dart`](jot_flutter/lib/main.dart) |
| Turn instructions and beginner guidance | [`lib/turn_guidance.dart`](jot_flutter/lib/turn_guidance.dart) |
| App state, local saves, computer scheduling, online sessions | [`lib/game_controller.dart`](jot_flutter/lib/game_controller.dart) |
| Client HTTP requests and invitation parsing | [`lib/multiplayer_api.dart`](jot_flutter/lib/multiplayer_api.dart) |
| Rules, scoring, and move validation | [`packages/jot_core/src/engine.cpp`](jot_flutter/packages/jot_core/src/engine.cpp) |
| Dictionary lookup or computer search | [`dictionary.cpp`](jot_flutter/packages/jot_core/src/dictionary.cpp), [`ai.cpp`](jot_flutter/packages/jot_core/src/ai.cpp) |
| Dart/native/browser transport | [`packages/jot_core/lib/src/`](jot_flutter/packages/jot_core/lib/src), [`web/`](jot_flutter/web) |
| Online rooms, persistence, or server validation | [`multiplayer/src/`](jot_flutter/multiplayer/src) |

Read [ARCHITECTURE.md](jot_flutter/ARCHITECTURE.md) before changing the boundary
between Flutter, the engine, and the server. Keep game rules in the shared C++
engine so local, browser, and online games agree.

## Run checks and tests

For ordinary Dart/UI work, start with:

```sh
flutter analyze
flutter test
```

The analyzer checks Dart code for errors and lint issues. Tests include widgets,
controllers, and the actual native C++ transport, so the host C++ toolchain must
be available. To focus on one suite, run, for example:

```sh
flutter test test/beginner_guidance_test.dart
```

For C++ changes, run the independent engine tests with CMake 3.16 or newer and
a C++17 compiler:

```sh
cmake -S packages/jot_core -B build/core -DCMAKE_BUILD_TYPE=Release -DBUILD_TESTING=ON
cmake --build build/core --config Release --parallel
ctest --test-dir build/core -C Release --output-on-failure
```

The complete CI verification command is:

```sh
python3 tools/run_tests.py
```

This larger pipeline also needs Python 3, CMake, Clang and matching LLVM coverage
tools (`llvm-cov`, `llvm-profdata`), activated Emscripten 4.0.13, Node.js 24 or
newer with npm, and Chrome. CI runs it on Ubuntu; its coverage scripts assume
Unix-style tooling. For Windows development, start with the Flutter and CMake
commands above. See [TESTING.md](jot_flutter/TESTING.md) for full setup, focused
commands, Chrome configuration, reports, and coverage requirements. The complete
pipeline installs npm dependencies, rebuilds WASM, and runs local server tests.
It does not deploy or contact production.

When contributing, add or update tests for behavior you change, format edited
Dart files with `dart format path/to/file.dart`, and run the relevant checks.
Describe the change and the checks you ran in your pull request. GitHub Actions
runs additional tests and platform builds; see the
[app README](jot_flutter/README.md#github-actions) for the workflow matrix.

## Work on online multiplayer

You can work on local gameplay without installing the backend tooling. For
online development, the server is a Cloudflare Worker with one SQLite-backed
Durable Object per game room. Node.js 24+ and npm are needed in addition to the
web build tools above. Wrangler, the local Cloudflare CLI, is pinned in the
server's npm dependencies.

From `jot_flutter`, prepare the server and web app:

```sh
npm --prefix multiplayer ci
python3 tools/build_web_core.py
python3 tools/build_server_core.py
flutter pub get
flutter build web --release
```

Start the local Worker in one terminal:

```sh
cd multiplayer
npm run dev -- --var PUBLIC_ORIGIN:http://localhost:8088
```

In a second terminal, from `jot_flutter`, start the Pages server and API proxy:

```sh
multiplayer/node_modules/.bin/wrangler pages dev build/web --port 8088
```

On Windows Command Prompt, invoke the same local executable as
`multiplayer\node_modules\.bin\wrangler.cmd pages dev build/web --port 8088`.
Keep both servers running, open [localhost:8088](http://localhost:8088/), choose
**Online game**, and open the invitation in a second browser profile or private
window so each player has separate saved credentials. These servers use local
storage separate from production. After editing the Flutter UI, rebuild the web
release; after C++ changes, rebuild both WASM targets and the web release and
restart the servers.

Browser clients use their current origin for `/api` requests. The regular
`flutter run -d chrome` server has no Pages API proxy. Native clients default to
the published API at `https://jot-game.pages.dev`; a compile-time
`--dart-define=JOT_API_BASE=http://localhost:8088` override can select your local
API when running a desktop client on the same computer. For browser development,
use the local Pages setup above; the production server rejects other browser
origins.

See [MULTIPLAYER.md](jot_flutter/MULTIPLAYER.md) for game/session behavior and
the [server README](jot_flutter/multiplayer/README.md) for API and test details.
Rebuild the server core after C++ or dictionary changes before server tests:

```sh
python3 tools/build_server_core.py
npm --prefix multiplayer run check
npm --prefix multiplayer test
```

## Build a release

`flutter run` normally uses debug mode for development. A release build produces
an optimized bundle for your chosen target:

| Target | Command | Output under `jot_flutter/` |
| --- | --- | --- |
| Web | `python3 tools/build_web_core.py`, then `flutter build web --release` | `build/web/` |
| Android debug APK for testing | `flutter build apk --debug` | `build/app/outputs/flutter-apk/app-debug.apk` |
| macOS | `flutter build macos --release` | `build/macos/Build/Products/Release/Jot.app` |
| Windows | `flutter build windows --release` | `build/windows/x64/runner/Release/` |
| Linux | `flutter build linux --release` | `build/linux/x64/release/bundle/` on x64 |

Desktop builds require their matching host operating system. Keep the complete
Windows/Linux output directory together; the executable needs its accompanying
libraries and data. Android release builds require a configured release key to
produce installable signed artifacts, and iOS distribution requires Apple
signing/provisioning. The [app README](jot_flutter/README.md#github-actions)
documents release signing and downloadable CI artifacts.

Publishing the web app and online server also needs the appropriate Cloudflare
account access. Follow [DEPLOYMENT.md](jot_flutter/DEPLOYMENT.md) for that process.

## Troubleshooting

| Symptom | What to check |
| --- | --- |
| `flutter` or `dart` command not found, or wrong version | Add the pinned SDK's `bin` directory to `PATH`, reopen the terminal, and check `flutter --version`. Use `which flutter` on macOS/Linux or `where flutter` in Windows Command Prompt to find competing SDKs. |
| “No pubspec.yaml file found” | Change into `jot_game/jot_flutter`, rather than the repository root. |
| No suitable device | Run `flutter doctor -v` and `flutter devices`. Start an emulator/simulator or authorize the connected device. Desktop targets require the matching host OS. |
| C++/native asset build fails | Check the chosen target's compiler and SDK/NDK setup. Windows needs Visual Studio's C++ workload; Apple targets need full Xcode. Read the first build error in the terminal. |
| Android licenses or SDK tools missing | Install the requested tools through SDK Manager and run `flutter doctor --android-licenses`. |
| Xcode or CocoaPods errors | Complete the linked Apple setup guide, including Xcode first launch, licenses, and CocoaPods; rerun `flutter doctor -v`. |
| “Install Emscripten and activate its SDK” | Activate 4.0.13 in the same terminal and verify `em++ --version` before running the build script. |
| Browser cannot load `jot_core.js` or `jot_core.wasm` | Run `python3 tools/build_web_core.py` before starting Flutter. Both files are generated and absent on a fresh checkout. |
| C++ edits do not appear | Stop and rerun native builds. For web/server, rebuild the relevant WASM target and relaunch/reload the app or server. Hot reload updates Dart code. |
| Online games fail on the Flutter browser dev server | Use the two-server local Pages/Worker setup above, which provides `/api` and the correct invitation origin. |
| Full coverage checks cannot find Chrome | Set `CHROME_EXECUTABLE` to the Chrome binary path, as described in [TESTING.md](jot_flutter/TESTING.md). |

If dependency/build caches appear stale after changing SDKs, stop the running
app, run `flutter clean`, then `flutter pub get`, and try the run command again.
Rebuild WASM separately for web. If you need help, include your OS, target,
`flutter --version`, relevant `flutter doctor -v` output, exact command, and
first error message. Remove credentials and private invitation links from logs.

## Further reading and licensing

| Guide | What it covers |
| --- | --- |
| [Flutter app README](jot_flutter/README.md) | Engine details, gameplay rules, CI artifacts, signing, and migration scope. |
| [Architecture](jot_flutter/ARCHITECTURE.md) | Responsibility boundaries and the migration from the original apps. |
| [Testing](jot_flutter/TESTING.md) | Test suites, coverage gates, and optional performance measurements. |
| [Online games](jot_flutter/MULTIPLAYER.md) | Invitations, privacy, session recovery, and game expiration. |
| [Server README](jot_flutter/multiplayer/README.md) | Server build, HTTP protocol, persistence, and tests. |
| [Deployment](jot_flutter/DEPLOYMENT.md) | Cloudflare publishing, local server setup, and deployment history. |
| [Engine API](jot_flutter/packages/jot_core/README.md) | C ABI ownership and JSON commands. |
| [Dictionary provenance](jot_flutter/assets/dictionary/README.md) | Original word-list source and reproducible export. |
| [Verification record](jot_flutter/VERIFICATION.md) | Recorded checks and their limits, including physical-device validation. |

Accounts, rankings, messaging, advertisements, and importing legacy saved games
remain future migration work. The repository contains no top-level license
grant; the original source and asset rights remain unchanged. The dictionary
conversion does not grant additional rights. Vendored nlohmann/json retains its
[upstream MIT license](jot_flutter/packages/jot_core/src/third_party/LICENSE.nlohmann-json).
