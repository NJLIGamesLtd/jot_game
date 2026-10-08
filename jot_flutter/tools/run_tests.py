#!/usr/bin/env python3
"""Run unit, regression, integration and widget/UI suites plus full coverage gates."""
import argparse
from pathlib import Path
import shutil
import subprocess

ROOT = Path(__file__).resolve().parents[1]


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('--flutter', default=shutil.which('flutter'))
    args = parser.parse_args()
    if not args.flutter:
        parser.error('Flutter is required; pass --flutter /path/to/flutter/bin/flutter')
    flutter = Path(args.flutter).resolve()
    dart = flutter.with_name('dart')
    commands = [
        ['python3', 'tools/test_coverage_gate.py'],
        [str(flutter), 'pub', 'get'],
        [str(flutter), 'analyze'],
        ['python3', 'tools/run_cpp_coverage.py'],
        [str(flutter), 'test', '--coverage', '--branch-coverage',
         '--coverage-package', 'jot_flutter', '--coverage-package', 'jot_core'],
        [str(flutter), 'test', '--platform', 'chrome',
         'test/platform_drag_test.dart', 'test/beginner_guidance_test.dart',
         'test/board_rendering_regression_test.dart',
         'test/board_cache_interaction_test.dart', 'test/board_layout_test.dart'],
        ['npm', '--prefix', 'tools', 'ci'],
        ['npm', '--prefix', 'tools', 'run', 'coverage'],
        ['python3', 'tools/run_dart_web_coverage.py', '--dart', str(dart)],
        ['python3', 'tools/build_web_core.py'],
        ['node', 'tools/test_web_core.cjs'],
        ['python3', 'tools/build_server_core.py'],
        ['node', '--test', 'packages/jot_core/tests/server_wasm_tests.mjs'],
        ['npm', '--prefix', 'multiplayer', 'ci'],
        ['npm', '--prefix', 'multiplayer', 'run', 'check'],
        ['npm', '--prefix', 'multiplayer', 'test'],
        ['python3', 'tools/check_coverage.py'],
    ]
    for command in commands:
        print('\nRunning: ' + ' '.join(command), flush=True)
        subprocess.run(command, cwd=ROOT, check=True)


if __name__ == '__main__':
    main()
