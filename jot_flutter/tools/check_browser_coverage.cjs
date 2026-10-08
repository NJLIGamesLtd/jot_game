'use strict';
const assert = require('node:assert/strict');
const fs = require('node:fs');
const path = require('node:path');

const summary = JSON.parse(fs.readFileSync(path.resolve(__dirname, '../coverage/browser/coverage-summary.json'), 'utf8'));
// c8's threshold check alone accepts a report with no files. Assert that every
// authored browser/Pages module is present and has executable coverage records.
const authored = fs.readdirSync(path.resolve(__dirname, '../web'))
  .filter((name) => name.endsWith('.js') && name !== 'jot_core.js');
assert.deepEqual(authored.sort(), ['_worker.js', 'jot_bridge.js', 'jot_worker.js']);
for (const filename of authored) {
  const key = path.resolve(__dirname, '../web', filename);
  const coverage = summary[key];
  assert.ok(coverage, `Missing source from browser coverage: ${key}`);
  for (const metric of ['lines', 'statements', 'functions', 'branches']) {
    assert.ok(coverage[metric].total > 0, `Empty ${metric} coverage for ${filename}`);
    assert.equal(coverage[metric].covered, coverage[metric].total, `${filename} ${metric} coverage must be exactly 100%`);
  }
}
console.log('All authored browser/Pages modules have 100% line, statement, function, and branch coverage.');
