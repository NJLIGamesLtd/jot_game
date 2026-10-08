"""Regression checks for malformed, partial, empty and incomplete reports."""
import importlib.util
import copy
from pathlib import Path
import tempfile
import unittest

SPEC = importlib.util.spec_from_file_location('coverage_gate', Path(__file__).with_name('check_coverage.py'))
GATE = importlib.util.module_from_spec(SPEC)
SPEC.loader.exec_module(GATE)


class CoverageGateTest(unittest.TestCase):
    def parse(self, contents):
        with tempfile.TemporaryDirectory() as directory:
            report = Path(directory) / 'lcov.info'
            report.write_text(contents)
            return GATE.read_lcov(report)

    def test_valid_records_merge_positive_counts(self):
        records = self.parse('SF:lib/main.dart\nDA:1,0\nDA:2,2\nBRDA:2,0,0,-\nLF:2\nLH:1\nend_of_record\n'
                             'SF:lib/main.dart\nDA:1,1\nLF:1\nLH:1\nend_of_record\n')
        value = records[GATE.ROOT / 'lib/main.dart']
        self.assertEqual(value['lines'], {1: 1, 2: 2})
        self.assertEqual(value['branches'], {('2', '0', '0'): 0})

    def test_llvm_macro_summary_can_exceed_emitted_line_rows(self):
        records = self.parse('SF:lib/main.dart\nDA:1,1\nLF:2\nLH:2\nend_of_record\n')
        self.assertEqual(records[GATE.ROOT / 'lib/main.dart']['lines'], {1: 1})

    def test_empty_partial_or_malformed_reports_fail(self):
        for contents in ['', 'DA:1,1\n', 'BRDA:1,0,0,1\n', 'SF:\n', 'end_of_record\n',
                         'SF:lib/main.dart\nDA:1,1\n', 'SF:lib/main.dart\nSF:lib/main.dart\n',
                         'SF:lib/main.dart\nDA:0,1\nend_of_record\n',
                         'SF:lib/main.dart\nDA:1,-1\nend_of_record\n',
                         'SF:lib/main.dart\nBRDA:1,0,0,-1\nend_of_record\n',
                         'SF:lib/main.dart\nLF:-1\nend_of_record\n',
                         'SF:lib/main.dart\nDA:1,1\nLF:0\nend_of_record\n',
                         'SF:lib/main.dart\nDA:1,1\nLH:0\nend_of_record\n',
                         'SF:lib/main.dart\nDA:1,1\nLF:1\nLH:2\nend_of_record\n']:
            with self.subTest(contents=contents), self.assertRaises(ValueError):
                self.parse(contents)

    def test_zero_negative_missing_and_empty_runtime_coverage_fail(self):
        source = GATE.ROOT / 'lib/main.dart'
        expected = {source}
        for lines in [{}, {1: 0}, {1: -1}]:
            with self.subTest(lines=lines):
                result, errors = GATE.evaluate('component', expected, {source: {'lines': lines, 'branches': {}}})
                self.assertTrue(errors)
                self.assertLess(result['linePercent'], 100)
        self.assertTrue(GATE.evaluate('component', expected, {})[1])
        self.assertTrue(GATE.evaluate('component', set(), {})[1])
        self.assertFalse(GATE.evaluate('component', expected, {source: {'lines': {1: 1}, 'branches': {}}})[1])

    def test_native_summary_requires_exact_positive_counts_and_membership(self):
        source = GATE.ROOT / 'packages/jot_core/src/engine.cpp'
        valid = {'files': [{'file': str(source), 'summary': {'lines': {'count': 2, 'covered': 2}}}],
                 'totals': {'lines': {'count': 2, 'covered': 2}}}
        GATE.validate_native_line_summary(valid, {source})
        mutations = [
            lambda report: report['files'].clear(),
            lambda report: report['files'].append(copy.deepcopy(report['files'][0])),
            lambda report: report['files'][0].update(file='packages/jot_core/src/new.cpp'),
            lambda report: report['files'][0]['summary']['lines'].update(count=0, covered=0),
            lambda report: report['files'][0]['summary']['lines'].update(count=100000, covered=99999, percent=100),
            lambda report: report['files'][0]['summary']['lines'].update(count=-1, covered=-1),
            lambda report: report['totals']['lines'].update(count=0, covered=0),
            lambda report: report['totals']['lines'].update(count=100000, covered=99999, percent=100),
            lambda report: report['totals']['lines'].update(count=3, covered=3),
        ]
        for mutate in mutations:
            report = copy.deepcopy(valid)
            mutate(report)
            with self.subTest(report=report), self.assertRaises(ValueError):
                GATE.validate_native_line_summary(report, {source})


if __name__ == '__main__':
    unittest.main()
