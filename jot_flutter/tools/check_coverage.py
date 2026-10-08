#!/usr/bin/env python3
"""Require exact 100% line coverage and a complete runtime source inventory."""
import argparse
import hashlib
import json
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
# These Dart libraries contain exports, external native declarations, or an
# abstract interface. Hashes make adding executable code invalidate that status.
DECLARATIONS = {
    'packages/jot_core/lib/jot_core.dart': 'cbf290d8b94ae4e78366ec6f8bfc486d0acecd755a2f16480028d18657bb5770',
    'packages/jot_core/lib/src/bindings.dart': 'e7552b2b7ea64787e95a1719f2df6f1cdb286367f2f4686b46d745521f489204',
    'packages/jot_core/lib/src/transport.dart': 'd7cc03c618ce38c7397788361be9289f200eeae3a127b6c577f6f732985427b5',
}
CPP_DECLARATIONS = {
    'packages/jot_core/src/ai.hpp': 'b7d02b1285772e0e23a20daa57ff6bfa2d6dee7bb4ba95e2a7c02ac198677a13',
    'packages/jot_core/src/jot_api.h': 'dfa94ae539de6482380a1336fa48be2d39422368ed188f07c776c463b27cd149',
    'packages/jot_core/src/abi_boundary.hpp': '56d1375fba62f8f390e0841f4ee58a819e86ba3b8034b3057c594113dc4d231c',
}


def read_lcov(path, base=ROOT):
    records = {}
    current = None
    record_lines = {}
    declared_lines = declared_hits = None
    for row in path.read_text().splitlines():
        if row.startswith('SF:'):
            if current is not None:
                raise ValueError(f'Unterminated source record in {path}')
            if not row[3:]:
                raise ValueError(f'Empty source filename in {path}')
            source = Path(row[3:])
            source = source if source.is_absolute() else base / source
            current = records.setdefault(source.resolve(), {'lines': {}, 'branches': {}})
            record_lines = {}
            declared_lines = declared_hits = None
        elif row.startswith('DA:'):
            if current is None:
                raise ValueError(f'Line without source in {path}')
            line, count, *_ = row[3:].split(',')
            line, count = int(line), int(count)
            if line <= 0 or count < 0:
                raise ValueError(f'Invalid coverage line or count in {path}: {row}')
            current['lines'][line] = current['lines'].get(line, 0) + count
            record_lines[line] = record_lines.get(line, 0) + count
        elif row.startswith('BRDA:'):
            if current is None:
                raise ValueError(f'Branch without source in {path}')
            line, block, branch, count = row[5:].split(',')
            if int(line) <= 0 or not block or not branch or (count != '-' and int(count) < 0):
                raise ValueError(f'Invalid coverage branch or count in {path}: {row}')
            key = (line, block, branch)
            current['branches'][key] = current['branches'].get(key, 0) + (0 if count == '-' else int(count))
        elif row.startswith(('LF:', 'LH:')):
            if current is None:
                raise ValueError(f'Line summary without source in {path}')
            number = int(row[3:])
            if number < 0:
                raise ValueError(f'Negative line summary in {path}: {row}')
            if row.startswith('LF:'):
                declared_lines = number
            else:
                declared_hits = number
        elif row == 'end_of_record':
            if current is None:
                raise ValueError(f'Record terminator without source in {path}')
            # LLVM's line summary can include covered lines omitted from its DA
            # list (for example, macro expansions). Do not require equality.
            if declared_lines is not None and declared_lines < len(record_lines):
                raise ValueError(f'Executable line summary disagrees with coverage records in {path}')
            if declared_hits is not None and declared_hits < sum(count > 0 for count in record_lines.values()):
                raise ValueError(f'Covered line summary disagrees with coverage records in {path}')
            if declared_lines is not None and declared_hits is not None and declared_hits > declared_lines:
                raise ValueError(f'Covered lines exceed executable lines in {path}')
            current = None
    if current is not None:
        raise ValueError(f'Unterminated source record in {path}')
    if not records:
        raise ValueError(f'Empty coverage report: {path}')
    return records


def validate_native_line_summary(summary, expected):
    """Require exact LLVM source membership and count equality, without rounding."""
    files = summary['files']
    found = set()
    line_count = covered_count = 0
    for entry in files:
        source = Path(entry['file'])
        source = (source if source.is_absolute() else ROOT / source).resolve()
        if source in found:
            raise ValueError(f'Duplicate C++ summary source: {source}')
        found.add(source)
        lines = entry['summary']['lines']
        count, covered = lines['count'], lines['covered']
        if type(count) is not int or type(covered) is not int or count <= 0 or covered != count:
            raise ValueError(f'C++ summary requires nonempty, exactly covered lines: {source}')
        line_count += count
        covered_count += covered
    if found != expected:
        raise ValueError('C++ summary files do not match executable source inventory')
    totals = summary['totals']['lines']
    if (type(totals['count']) is not int or type(totals['covered']) is not int
            or totals['count'] <= 0 or totals['covered'] != totals['count']
            or totals['count'] != line_count or totals['covered'] != covered_count):
        raise ValueError('C++ aggregate summary requires nonempty, exactly covered line totals')


def evaluate(name, expected, records):
    missing = sorted(str(p.relative_to(ROOT)) for p in expected - set(records))
    failures = [f'{name}: missing source {p}' for p in missing]
    if not expected:
        failures.append(f'{name}: empty runtime source inventory')
    covered = total = branches = branch_hits = 0
    files = []
    for source in sorted(expected & set(records)):
        lines = records[source]['lines']
        hits = sum(value > 0 for value in lines.values())
        missed = sorted(line for line, value in lines.items() if value <= 0)
        if not lines:
            failures.append(f'{name}: empty executable coverage for {source.relative_to(ROOT)}')
        if missed:
            failures.append(f'{name}: uncovered {source.relative_to(ROOT)} lines {missed}')
        if hits != len(lines) and not missed:
            failures.append(f'{name}: coverage hit counts are invalid for {source.relative_to(ROOT)}')
        covered += hits
        total += len(lines)
        branch_records = records[source]['branches']
        branches += len(branch_records)
        branch_hits += sum(value > 0 for value in branch_records.values())
        files.append({'source': str(source.relative_to(ROOT)), 'covered': hits,
                      'lines': len(lines), 'uncovered': missed})
    return {'component': name, 'covered': covered, 'lines': total,
            'linePercent': 100 * covered / total if total else 0,
            'branchesCovered': branch_hits, 'branches': branches,
            'branchPercent': 100 * branch_hits / branches if branches else None,
            'files': files}, failures


def check():
    cpp = ROOT / 'packages/jot_core/src'
    cpp_sources = {p.resolve() for p in cpp.rglob('*') if p.suffix in {'.cpp', '.hpp', '.h'}
                   and 'third_party' not in p.relative_to(cpp).parts}
    cpp_summary = json.loads((ROOT / 'coverage/cpp/summary.json').read_text())
    cpp_inventory = {ROOT / p for p in cpp_summary['sourceInventory']}
    if cpp_sources != cpp_inventory:
        raise ValueError('C++ coverage inventory does not match first-party source files')
    cpp_declarations = {ROOT / p for p in cpp_summary['noExecutableLines']}
    if cpp_declarations != {ROOT / name for name in CPP_DECLARATIONS}:
        raise ValueError('Unexpected C++ declaration-only files; review coverage scope')
    validate_native_line_summary(cpp_summary, cpp_sources - cpp_declarations)
    for path, expected_hash in CPP_DECLARATIONS.items():
        if hashlib.sha256((ROOT / path).read_bytes()).hexdigest() != expected_hash:
            raise ValueError(f'Declaration-only C++ header changed; review coverage: {path}')
    dart_sources = {p.resolve() for folder in (ROOT / 'lib', ROOT / 'packages/jot_core/lib')
                    for p in folder.rglob('*.dart')}
    for path, expected_hash in DECLARATIONS.items():
        source = ROOT / path
        if hashlib.sha256(source.read_bytes()).hexdigest() != expected_hash:
            raise ValueError(f'Declaration-only library changed; review executable coverage: {path}')
        dart_sources.remove(source)
    web_dart = {ROOT / 'packages/jot_core/lib/src/bridge_web.dart'}
    browser_sources = {p.resolve() for p in (ROOT / 'web').rglob('*.js')
                       if p != ROOT / 'web/jot_core.js'}
    backend_sources = {p.resolve() for p in (ROOT / 'multiplayer/src').rglob('*')
                       if p.suffix in {'.ts', '.js', '.mjs'} and not p.name.endswith('.d.ts')}
    backend_sources |= {p.resolve() for p in (ROOT / 'multiplayer').glob('*.mjs')}
    components = [
        ('C++ rules / AI / dictionary / C ABI', cpp_sources - cpp_declarations,
         ROOT / 'coverage/cpp/lcov.info', ROOT),
        ('Flutter UI / controllers / HTTP / native transport', dart_sources - web_dart,
         ROOT / 'coverage/lcov.info', ROOT),
        ('Dart web transport (source-mapped JS)', web_dart,
         ROOT / 'coverage/dart-web/lcov.info', ROOT),
        ('Browser worker / bridge / Pages proxy', browser_sources,
         ROOT / 'coverage/browser/lcov.info', ROOT / 'tools'),
        ('Multiplayer Worker / Durable Objects / WASM adapter', backend_sources,
         ROOT / 'coverage/multiplayer/lcov.info', ROOT / 'multiplayer'),
    ]
    results = []
    failures = []
    for name, expected, report, base in components:
        stale = [str(source.relative_to(ROOT)) for source in expected
                 if source.stat().st_mtime_ns > report.stat().st_mtime_ns]
        if stale:
            raise ValueError(f'{name}: source changed after its coverage report: {stale}')
        result, errors = evaluate(name, expected, read_lcov(report, base))
        if report == ROOT / 'coverage/cpp/lcov.info':
            result['compilerSummary'] = cpp_summary['totals']
        results.append(result)
        failures.extend(errors)
    summary = {'scope': 'All first-party runtime source of the C++/Flutter rewrite',
               'components': results, 'declarationOnlyDart': list(DECLARATIONS),
               'declarationOnlyCpp': cpp_summary['noExecutableLines'],
               'excludedDependencies': ['Flutter/Dart SDK', 'npm packages', 'third_party/json.hpp'],
               'separateValidation': ['Generated WASM/JS', 'platform runners and build/deployment tools'],
               'failures': failures}
    directory = ROOT / 'coverage'
    directory.mkdir(exist_ok=True)
    (directory / 'summary.json').write_text(json.dumps(summary, indent=2) + '\n')
    table = ['# Jot runtime test coverage', '',
             '| Component | Covered lines | Line coverage | Branch coverage |',
             '| --- | ---: | ---: | ---: |']
    for result in results:
        compiler = result.get('compilerSummary')
        covered = compiler['lines']['covered'] if compiler else result['covered']
        lines = compiler['lines']['count'] if compiler else result['lines']
        branch_percent = compiler['branches']['percent'] if compiler else result['branchPercent']
        branch = 'not reported' if branch_percent is None else f"{branch_percent:.2f}%"
        table.append(f"| {result['component']} | {covered}/{lines} | {100 * covered / lines:.2f}% | {branch} |")
        print(f"{result['component']}: {covered}/{lines} lines; branches {branch}")
    table += ['', '100% means every reported coverage line is hit and every authored runtime file is accounted for; no rounding or coverage-ignore directives.',
              'Native Dart and C++ use their executable-line coverage. Dart web values use dart2js source maps and compiled JavaScript counters; their denominator differs from Dart VM executable-line and branch coverage.',
              'Source-map coverage describes paths retained by this compilation and does not establish coverage of code a compiler eliminates or folds. The web harness explicitly exercises every authored bridge method.',
              'The C++ table uses the LLVM compiler summary; its macro coverage totals differ slightly from emitted LCOV line/branch rows. Both are independently checked.',
              'Declaration-only libraries have no executable lines. Generated artifacts and platform/build tooling are validated separately.', '']
    if failures:
        table += ['## Gate failures', ''] + ['- ' + failure for failure in failures]
    (directory / 'summary.md').write_text('\n'.join(table) + '\n')
    if failures:
        raise ValueError('\n'.join(failures))
    print('PASS: every authored runtime source is accounted for and has exactly 100% line coverage.')


def main():
    argparse.ArgumentParser(description=__doc__).parse_args()
    try:
        check()
    except (ValueError, OSError, KeyError) as error:
        raise SystemExit(f'Coverage gate failed: {error}') from error


if __name__ == '__main__':
    main()
