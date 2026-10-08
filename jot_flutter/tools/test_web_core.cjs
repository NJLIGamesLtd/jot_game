#!/usr/bin/env node
'use strict';

// Run after tools/build_web_core.py. An optional native shared-library path
// enables a WASM -> native -> WASM save round trip through the public C ABI.
// Example: node tools/test_web_core.cjs build/core/libjot_core.dylib
const assert = require('node:assert/strict');
const fs = require('node:fs');
const os = require('node:os');
const path = require('node:path');
const {execFileSync} = require('node:child_process');

const project = path.resolve(__dirname, '..');
const dictionaryPath = path.join(project, 'assets/dictionary/words.txt');
const createModule = require(path.join(project, 'web/jot_core.js'));

async function main() {
  const module = await createModule({
    wasmBinary: fs.readFileSync(path.join(project, 'web/jot_core.wasm')),
  });
  const create = module.cwrap('jot_create', 'number', ['string', 'number', 'number']);
  const execute = module.cwrap('jot_execute', 'string', ['number', 'string']);
  const destroy = module.cwrap('jot_destroy', null, ['number']);
  const dictionary = fs.readFileSync(dictionaryPath, 'ascii');
  const started = performance.now();
  // Passing the full 1.29 MB asset exercises cwrap's temporary stack allocation.
  const first = create(dictionary, 123, 1);
  const second = create(dictionary, 987, -1);
  assert(first && second, 'WASM must initialize with the complete original dictionary');
  const command = (handle, value) => JSON.parse(execute(handle, JSON.stringify(value)));
  try {
    const initial = command(first, {op: 'state'});
    assert.equal(initial.state.dictionaryWords, 143722);
    assert.equal(initial.state.size, 16);
    assert.equal(initial.state.rack.length, 0);
    const untouchedSecond = command(second, {op: 'save'}).save;
    const filled = command(first, {op: 'fill'});
    assert(filled.ok);
    assert.equal(filled.state.rack.length, 8);
    const beforeHint = command(first, {op: 'save'}).save;
    const hint = command(first, {op: 'hint'});
    assert(hint.ok && hint.hint.placements.length >= 2, 'Opening hint must be a legal word');
    assert.deepEqual(command(first, {op: 'save'}).save, beforeHint, 'Hint search must be immutable');
    const staged = command(first, {op: 'hint', apply: true});
    assert(staged.ok && staged.state.preview.valid);
    const pendingSave = command(first, {op: 'save'}).save;
    assert(pendingSave.fullState.pending.length >= 2);
    assert(command(second, {op: 'restore', save: pendingSave}).ok);
    assert.deepEqual(command(second, {op: 'save'}).save, pendingSave, 'Pending placements must round trip');
    // Restore the second handle's own initial state before exercising the robot.
    assert(command(second, {op: 'restore', save: untouchedSecond}).ok);
    const played = command(first, {op: 'play'});
    assert(played.ok);
    assert.equal(played.state.currentPlayer, 1);
    const afterHuman = command(first, {op: 'save'}).save;
    const robot = command(first, {op: 'robot'});
    assert(robot.ok && robot.hint && robot.hint.placements.length > 0);
    assert.equal(robot.state.currentPlayer, 0);
    assert(robot.state.scores[1] > afterHuman.fullState.scores[1]);
    assert.deepEqual(command(second, {op: 'save'}).save, untouchedSecond, 'Game handles must not share mutable state');
    const saved = command(first, {op: 'save'}).save;
    assert(command(second, {op: 'restore', save: saved}).ok);
    assert.deepEqual(command(second, {op: 'save'}).save, saved);
    const invalidSave = structuredClone(saved);
    invalidSave.fullState.players[0].rack.push({letter: 'Q', blank: false, owner: 0});
    assert.equal(command(second, {op: 'restore', save: invalidSave}).ok, false);
    assert.deepEqual(command(second, {op: 'save'}).save, saved, 'Rejected restoration must not alter the game');
    assert.equal(command(second, {op: 'place', rack: -1, row: 7, col: 7, letter: 'A'}).ok, false);
    assert.deepEqual(command(second, {op: 'save'}).save, saved, 'Rejected placement must not alter the game');

    if (process.argv[2]) {
      const library = path.resolve(process.argv[2]);
      assert(fs.existsSync(library), `Native library does not exist: ${library}`);
      const temporary = fs.mkdtempSync(path.join(os.tmpdir(), 'jot-save-exchange-'));
      try {
        const executable = path.join(temporary, process.platform === 'win32' ? 'exchange.exe' : 'exchange');
        const compiler = process.env.CXX || 'c++';
        const args = ['-std=c++17', '-O2', '-I', path.join(project, 'packages/jot_core/src'),
          path.join(__dirname, 'native_save_roundtrip.cpp'), library, '-o', executable];
        if (process.platform !== 'win32') args.push(`-Wl,-rpath,${path.dirname(library)}`);
        execFileSync(compiler, args, {stdio: 'inherit'});
        const input = path.join(temporary, 'wasm-save.json');
        fs.writeFileSync(input, JSON.stringify(saved));
        const native = JSON.parse(execFileSync(executable, [dictionaryPath, input], {encoding: 'utf8'}));
        assert(native.ok);
        assert.deepEqual(native.save, saved, 'Native C++ must restore and preserve the WASM save');
        assert(command(second, {op: 'restore', save: native.save}).ok);
        assert.deepEqual(command(second, {op: 'save'}).save, native.save, 'WASM must restore the native C++ save');
        console.log('WASM -> native -> WASM save interoperability passed.');
      } finally {
        fs.rmSync(temporary, {recursive: true, force: true});
      }
    }
    console.log(`WASM smoke test passed with 143722 dictionary words (${Math.round(performance.now() - started)} ms).`);
  } finally {
    destroy(first);
    destroy(second);
  }
}

main().catch(error => {
  console.error(error);
  process.exitCode = 1;
});
