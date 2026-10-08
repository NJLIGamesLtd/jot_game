#!/usr/bin/env python3
"""Compare release Wasm size and fixed-game latency, serially, without deploying."""
import argparse
import json
from pathlib import Path
import shutil
import subprocess


ROOT = Path(__file__).resolve().parents[1]


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('--source-root', type=Path, default=ROOT / 'packages/jot_core/src')
    parser.add_argument('--output-dir', type=Path, default=ROOT / 'build/performance-flags')
    parser.add_argument('--initial-memory', type=int, default=16777216)
    args = parser.parse_args()
    compiler = shutil.which('em++')
    if not compiler:
        parser.error('Activate Emscripten before running this comparison.')
    destination = args.output_dir.resolve()
    destination.mkdir(parents=True, exist_ok=True)
    reports = {}
    for name, flags in [('o3', ['-O3']), ('o3-lto', ['-O3', '-flto']),
                        ('oz-lto', ['-Oz', '-flto'])]:
        output = destination / name
        output.mkdir(exist_ok=True)
        subprocess.run([
            compiler, '-std=c++17', *flags, '-fexceptions',
            *(str(args.source_root / source) for source in
              ('engine.cpp', 'dictionary.cpp', 'ai.cpp', 'jot_api.cpp', 'abi_boundary.cpp')),
            '-o', str(output / 'core.cjs'),
            '-sMODULARIZE=1', '-sEXPORT_NAME=createJotModule',
            '-sALLOW_MEMORY_GROWTH=1', '-sENVIRONMENT=web,worker,node',
            '-sDISABLE_EXCEPTION_CATCHING=0',
            '-sEXPORTED_FUNCTIONS=["_jot_create","_jot_execute","_jot_destroy"]',
            '-sEXPORTED_RUNTIME_METHODS=["cwrap"]',
            '-sSTACK_SIZE=2097152', f'-sINITIAL_MEMORY={args.initial_memory}',
        ], check=True)
        subprocess.run([
            'node', 'test/performance-benchmark.mjs', '--low-level-only',
            f'--module={output / "core.cjs"}', f'--wasm={output / "core.wasm"}',
            f'--dictionary={ROOT / "assets/dictionary/words.txt"}',
            '--count=40', '--hints=12', f'--output={output / "results.json"}',
        ], cwd=ROOT / 'multiplayer', check=True, stdout=subprocess.DEVNULL)
        report = json.loads((output / 'results.json').read_text())
        report['glueBytes'] = (output / 'core.cjs').stat().st_size
        report['flags'] = flags
        reports[name] = report
    for key in ('scenarioFingerprint', 'openingHintFingerprint', 'midgameHintFingerprint'):
        if len({report[key] for report in reports.values()}) != 1:
            raise SystemExit(f'Compiler variants changed game output: {key}')
    (destination / 'comparison.json').write_text(json.dumps(reports, indent=2) + '\n')
    print(json.dumps({name: {'wasmBytes': r['wasmBytes'], 'glueBytes': r['glueBytes'],
                            'metrics': r['metrics']} for name, r in reports.items()}, indent=2))


if __name__ == '__main__':
    main()
