import 'package:hooks/hooks.dart';
import 'package:code_assets/code_assets.dart';
import 'package:native_toolchain_c/native_toolchain_c.dart';

void main(List<String> args) async {
  await build(args, (input, output) async {
    if (!input.config.buildCodeAssets) return;
    final windows = input.config.code.targetOS == OS.windows;
    final android = input.config.code.targetOS == OS.android;
    final builder = CBuilder.library(
      name: 'jot_core',
      assetName: 'jot_core.dart',
      sources: [
        'src/engine.cpp',
        'src/dictionary.cpp',
        'src/ai.cpp',
        'src/abi_boundary.cpp',
        'src/jot_api.cpp',
      ],
      language: Language.cpp,
      std: 'c++17',
      // Bundle libc++ on Android so the app does not depend on an unregistered
      // companion libc++_shared.so being packaged by Flutter.
      cppLinkStdLib: android ? 'c++_static' : null,
      flags: windows ? ['/EHsc'] : ['-fvisibility=hidden'],
    );
    await builder.run(input: input, output: output);
  });
}
