#!/usr/bin/env python3
"""Compile and test the actual Dart JS-interop bridge, retaining source maps."""
import argparse
import json
from pathlib import Path
import shutil
import subprocess

ROOT = Path(__file__).resolve().parents[1]


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('--dart', default=shutil.which('dart'))
    args = parser.parse_args()
    if not args.dart:
        parser.error('Dart is required; pass --dart /path/to/flutter/bin/dart')
    output = ROOT / 'build/test-web-bridge/bridge_test.js'
    output.parent.mkdir(parents=True, exist_ok=True)
    subprocess.run([
        args.dart, 'compile', 'js', '--enable-asserts', '-O0',
        '-o', str(output), 'tools/web_bridge_test.dart',
    ], cwd=ROOT, check=True)
    subprocess.run([
        'node', 'tools/node_modules/c8/bin/c8.js', '--exclude-after-remap',
        '--include', '**/packages/jot_core/lib/src/bridge_web.dart',
        '--include', '**/packages/jot_core/lib/src/creation.dart',
        '--extension', '.dart', '--reporter', 'text', '--reporter', 'lcov',
        '--reporter', 'json-summary', '--reports-dir', 'coverage/dart-web',
        '--100', '--per-file', 'node', 'tools/test_dart_web_bridge.cjs',
    ], cwd=ROOT, check=True)
    report = json.loads((ROOT / 'coverage/dart-web/coverage-summary.json').read_text())
    expected = {
        str(ROOT / 'packages/jot_core/lib/src/bridge_web.dart'),
        str(ROOT / 'packages/jot_core/lib/src/creation.dart'),
    }
    actual = set(report) - {'total'}
    if actual != expected:
        raise SystemExit(f'Incomplete Dart web coverage: expected {expected}, got {actual}')
    for name in expected:
        for metric in ('lines', 'functions', 'branches'):
            values = report[name][metric]
            if values['total'] == 0 or values['covered'] != values['total']:
                raise SystemExit(f'Incomplete {metric} coverage for {name}: {values}')
    print('Actual Dart web bridge: all source files present; 100% mapped line/function/branch coverage.')


if __name__ == '__main__':
    main()
