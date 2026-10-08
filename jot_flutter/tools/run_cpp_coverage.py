#!/usr/bin/env python3
"""Build, run, and measure all first-party portable C++ source with LLVM coverage."""
import argparse
import json
import os
import pathlib
import shutil
import subprocess
import sys


ROOT = pathlib.Path(__file__).resolve().parents[1]


def tool(name):
    if sys.platform == 'darwin':
        return subprocess.check_output(['xcrun', '--find', name], text=True).strip()
    path = shutil.which(name)
    if not path:
        raise SystemExit(f'Install Clang and LLVM tools; {name} was not found.')
    return path


def run(arguments, **kwargs):
    subprocess.run([str(item) for item in arguments], check=True, cwd=ROOT, **kwargs)


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('--output', type=pathlib.Path, default=ROOT / 'coverage/cpp')
    parser.add_argument('--minimum', type=float, default=100.0,
                        help='Required first-party line coverage percentage (default: 100).')
    args = parser.parse_args()
    output = args.output.resolve()
    output.mkdir(parents=True, exist_ok=True)
    build = output / 'build'
    profiles = output / 'profiles'
    profiles.mkdir(exist_ok=True)
    for old_profile in profiles.glob('*.profraw'):
        old_profile.unlink()
    run(['cmake', '-S', ROOT / 'packages/jot_core', '-B', build,
         '-DBUILD_TESTING=ON', '-DJOT_ENABLE_COVERAGE=ON', '-DCMAKE_BUILD_TYPE=Debug',
         f'-DCMAKE_C_COMPILER={tool("clang")}', f'-DCMAKE_CXX_COMPILER={tool("clang++")}'])
    run(['cmake', '--build', build, '-j', '4'])
    environment = os.environ.copy()
    environment['LLVM_PROFILE_FILE'] = str(profiles / '%m-%p.profraw')
    run(['ctest', '--test-dir', build, '--output-on-failure'], env=environment)
    raw_profiles = list(profiles.glob('*.profraw'))
    if not raw_profiles:
        raise SystemExit('No LLVM profiles were produced by the test run.')
    merged = output / 'coverage.profdata'
    run([tool('llvm-profdata'), 'merge', '-sparse', *raw_profiles, '-o', merged])
    suffix = '.dylib' if sys.platform == 'darwin' else '.so'
    production_library = build / f'libjot_core{suffix}'
    sources = sorted(path for path in (ROOT / 'packages/jot_core/src').rglob('*')
                     if path.suffix in {'.cpp', '.hpp', '.h'} and 'third_party' not in path.parts)
    # This list includes every first-party engine/API/AI/dictionary source and
    # header. Only the separately licensed vendored JSON dependency is omitted.
    # The shipped library contains the canonical mapping for every production
    # translation unit. Profiles still merge direct rules/AI tests and C ABI
    # integration tests. Using one mapping avoids Clang's discarded duplicate
    # inline definitions (hash zero) from unrelated test translation units.
    common = [production_library, f'-instr-profile={merged}', *sources]
    covariance = tool('llvm-cov')
    run([covariance, 'report', *common])
    with (output / 'coverage.json').open('w') as result:
        run([covariance, 'export', *common], stdout=result)
    with (output / 'lcov.info').open('w') as result:
        run([covariance, 'export', *common, '-format=lcov'], stdout=result)
    run([covariance, 'show', *common, '-format=html', f'-output-dir={output / "html"}',
         '-show-line-counts-or-regions', '-show-branches=count'])
    data = json.loads((output / 'coverage.json').read_text())['data'][0]
    totals = data['totals']
    uncovered_branches = []
    for file in data['files']:
        lines = pathlib.Path(file['filename']).read_text().splitlines()
        for branch in file['branches']:
            if branch[4] == 0 or branch[5] == 0:
                uncovered_branches.append({
                    'file': str(pathlib.Path(file['filename']).relative_to(ROOT)),
                    'line': branch[0], 'column': branch[1],
                    'trueCount': branch[4], 'falseCount': branch[5],
                    'source': lines[branch[0] - 1].strip(),
                })
    (output / 'summary.json').write_text(json.dumps({
        'scope': 'Every first-party C++ source and header in packages/jot_core/src',
        'dependencyExclusions': ['packages/jot_core/src/third_party/json.hpp'],
        'sourceInventory': [str(path.relative_to(ROOT)) for path in sources],
        'noExecutableLines': [str(path.relative_to(ROOT)) for path in sources
                              if str(path) not in {file['filename'] for file in data['files']}],
        'totals': totals,
        'files': [{'file': file['filename'], 'summary': file['summary']} for file in data['files']],
        'uncoveredBranches': uncovered_branches,
    }, indent=2) + '\n')
    line = totals['lines']
    print(f'C++ line coverage: {line["covered"]}/{line["count"]} ({line["percent"]:.2f}%)')
    print(f'Report: {output / "html/index.html"}')
    if line['percent'] < args.minimum:
        raise SystemExit(f'Coverage is below the required {args.minimum:g}%.')


if __name__ == '__main__':
    main()
