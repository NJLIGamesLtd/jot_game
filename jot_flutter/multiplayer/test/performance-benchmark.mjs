// Serial, reproducible local measurements; no live service is contacted.
// Run npm run build first for HTTP measurements. Compiler comparisons can use:
// node test/performance-benchmark.mjs --low-level-only --module=/tmp/core.cjs \
//   --wasm=/tmp/core.wasm --dictionary=../assets/dictionary/words.txt
// Use --worker-directory=/tmp/worker-bundle to compare a separately built
// local HTTP implementation while retaining the same low-level measurements.
import assert from 'node:assert/strict';
import { createHash, randomUUID } from 'node:crypto';
import { readFile, writeFile, mkdtemp, rm } from 'node:fs/promises';
import { tmpdir } from 'node:os';
import { resolve, join } from 'node:path';
import { pathToFileURL } from 'node:url';
import { runtime } from './runtime.mjs';

const args = new Map(process.argv.slice(2).map((arg) => {
  const [key, ...parts] = arg.replace(/^--/, '').split('=');
  return [key, parts.length ? parts.join('=') : true];
}));
const count = Number(args.get('count') ?? 40);
const hintCount = Number(args.get('hints') ?? 3);
assert(Number.isInteger(count) && count > 0 && count <= 80);
assert(Number.isInteger(hintCount) && hintCount >= 0 && hintCount <= 20);
const modulePath = resolve(String(args.get('module') ?? 'generated/jot_server_core.mjs'));
const wasmPath = resolve(String(args.get('wasm') ?? 'generated/jot_server_core.wasm'));
const dictionaryPath = resolve(String(args.get('dictionary') ?? 'generated/jot_dictionary.txt'));
const workerDirectory = resolve(String(args.get('worker-directory') ?? 'dist'));
const factory = (await import(pathToFileURL(modulePath).href)).default;
const dictionary = await readFile(dictionaryPath, 'utf8');
const binary = await readFile(wasmPath);
const compiled = new WebAssembly.Module(binary);
let memory;
const instantiateWasm = (imports, receiveInstance) => {
  const instance = new WebAssembly.Instance(compiled, imports);
  memory = Object.values(instance.exports).find((value) => value instanceof WebAssembly.Memory);
  assert(memory);
  receiveInstance(instance, compiled);
  return instance.exports;
};
const report = {
  node: process.version, modulePath, wasmPath, workerDirectory, count, hintCount,
  dictionaryBytes: Buffer.byteLength(dictionary), wasmBytes: binary.length,
  dictionarySha256: createHash('sha256').update(dictionary).digest('hex'),
  wasmSha256: createHash('sha256').update(binary).digest('hex'),
  // RSS belongs to this Node benchmark process, including its multiple module
  // instances. It is not the memory footprint of a deployed Workers isolate.
  nodeProcessRssBeforeBytes: process.memoryUsage().rss,
  metrics: {},
};
const round = (n) => Math.round(n * 1000) / 1000;
function metric(name, samples) {
  const sorted = [...samples].sort((a, b) => a - b);
  report.metrics[name] = {
    samples: samples.length, minMs: round(sorted[0]),
    p50Ms: round(sorted[Math.floor(sorted.length * .5)]),
    p95Ms: round(sorted[Math.min(sorted.length - 1, Math.floor(sorted.length * .95))]),
    meanMs: round(samples.reduce((a, b) => a + b, 0) / samples.length),
  };
}
function measure(name, operation, repetitions = count) {
  const samples = [];
  for (let i = 0; i < repetitions; i++) {
    const start = performance.now();
    operation();
    samples.push(performance.now() - start);
  }
  metric(name, samples);
}
function checked(game, command) {
  const result = game.execute(command);
  assert.equal(result.ok, true, `${JSON.stringify(command)}: ${result.message}`);
  return result;
}
function prepareRack(state, owner, letters) {
  const player = state.players[owner];
  for (const letter of letters) {
    const bag = 'AEIOU@'.includes(letter) ? 'vowels' : 'consonants';
    const index = player[bag].indexOf(letter);
    assert(index >= 0);
    player[bag] = player[bag].slice(0, index) + player[bag].slice(index + 1);
    player.rack.push({ letter, blank: letter === '@', owner });
  }
}
function scenario(game) {
  let save = checked(game, { op: 'save' }).save;
  save.fullState.currentPlayer = 1;
  prepareRack(save.fullState, 0, 'JRABC');
  prepareRack(save.fullState, 1, 'EARNINGB');
  checked(game, { op: 'restore', save });
  for (let i = 0; i < 7; i++) checked(game, { op: 'place', rack: i, row: 8, col: 3 + i, letter: 'EARNING'[i] });
  checked(game, { op: 'play' });
  checked(game, { op: 'pass' });
  save = checked(game, { op: 'save' }).save;
  prepareRack(save.fullState, 1, 'PAT');
  checked(game, { op: 'restore', save });
  checked(game, { op: 'fill' });
  for (let i = 0; i < 3; i++) checked(game, { op: 'place', rack: i + 1, row: 5 + i, col: 3, letter: 'PAT'[i] });
  checked(game, { op: 'play' });
  checked(game, { op: 'place', rack: 0, row: 6, col: 2, letter: 'J' });
  checked(game, { op: 'place', rack: 1, row: 6, col: 4, letter: 'R' });
  assert.equal(checked(game, { op: 'view', player: 0 }).state.preview.score, 19);
  return checked(game, { op: 'save' }).save;
}

let start = performance.now();
const module = await factory({ wasmBinary: binary, instantiateWasm });
metric('moduleInitialization', [performance.now() - start]);
const create = module.cwrap('jot_create', 'number', ['string', 'number', 'number']);
const execute = module.cwrap('jot_execute', 'string', ['number', 'string']);
const destroy = module.cwrap('jot_destroy', null, ['number']);
function rawGame(seed = 314159) {
  let handle = create(dictionary, seed, -1);
  assert(handle);
  return {
    execute(command) { return JSON.parse(execute(handle, JSON.stringify(command))); },
    close() { if (handle) destroy(handle); handle = 0; },
  };
}
report.wasmHeapBeforeCreate = memory.buffer.byteLength;
start = performance.now();
const first = rawGame();
metric('firstDictionaryAndGameCreation', [performance.now() - start]);
const saved = scenario(first);
report.savedGameBytes = Buffer.byteLength(JSON.stringify(saved));
report.wasmHeapAfterScenario = memory.buffer.byteLength;
const state = checked(first, { op: 'view', player: 0 }).state;
report.scenarioFingerprint = createHash('sha256').update(JSON.stringify(state)).digest('hex');
first.close();
measure('legacyCreateDestroy', () => { const game = rawGame(); game.close(); });
measure('legacyCreateRestoreViewDestroy', () => {
  const game = rawGame();
  try { checked(game, { op: 'restore', save: saved }); checked(game, { op: 'view', player: 0 }); }
  finally { game.close(); }
});
const active = rawGame();
try {
  checked(active, { op: 'restore', save: saved });
  measure('nativeViewAndJsonDecode', () => checked(active, { op: 'view', player: 0 }));
  report.wasmHeapAfterRepeatedHandles = memory.buffer.byteLength;
  assert.equal(report.wasmHeapAfterRepeatedHandles, report.wasmHeapAfterScenario);
  if (hintCount > 0) {
    const opening = rawGame(12345);
    try {
      checked(opening, { op: 'fill' });
      const hint = checked(opening, { op: 'hint' }).hint;
      report.openingHintFingerprint = createHash('sha256').update(JSON.stringify(hint)).digest('hex');
      measure('openingHint', () => assert.deepEqual(checked(opening, { op: 'hint' }).hint, hint), hintCount);
    } finally { opening.close(); }
    checked(active, { op: 'recall' }); checked(active, { op: 'fill' });
    const hint = checked(active, { op: 'hint' }).hint;
    report.midgameHintFingerprint = createHash('sha256').update(JSON.stringify(hint)).digest('hex');
    measure('midgameHint', () => assert.deepEqual(checked(active, { op: 'hint' }).hint, hint), hintCount);
  }
} finally { active.close(); }
report.wasmHeapAfterHints = memory.buffer.byteLength;
report.nodeProcessRssAfterLowLevelBytes = process.memoryUsage().rss;

// Exercise the actual handwritten adapter in Node by substituting only the
// Workers-specific precompiled-Wasm and Text imports. Its runtime code is intact.
if (!args.has('low-level-only')) {
  const directory = await mkdtemp(join(tmpdir(), 'jot-performance-'));
  let mf;
  try {
    let source = await readFile('server_core.mjs', 'utf8');
    source = source.replace("import createJotModule from './generated/jot_server_core.mjs';", `import createJotModule from ${JSON.stringify(pathToFileURL(modulePath).href)};`)
      .replace("import wasmModule from './generated/jot_server_core.wasm';", 'const wasmModule = globalThis.__jotBenchmarkWasm;')
      .replace("import dictionary from './generated/jot_dictionary.txt';", 'const dictionary = globalThis.__jotBenchmarkDictionary;');
    globalThis.__jotBenchmarkWasm = compiled;
    globalThis.__jotBenchmarkDictionary = dictionary;
    const adapterPath = join(directory, 'adapter.mjs');
    await writeFile(adapterPath, source);
    const adapter = await import(pathToFileURL(adapterPath).href);
    start = performance.now();
    const game = await adapter.createGame(314159, saved);
    metric('adapterColdCreateRestore', [performance.now() - start]);
    assert.deepEqual(checked(game, { op: 'view', player: 0 }).state, state);
    game.close();
    const samples = [];
    for (let i = 0; i < count; i++) {
      start = performance.now();
      const game = await adapter.createGame(314159, saved);
      try { checked(game, { op: 'view', player: 0 }); } finally { game.close(); }
      samples.push(performance.now() - start);
    }
    metric('adapterWarmCreateRestoreViewDestroy', samples);
    report.nodeProcessRssAfterAdapterBytes = process.memoryUsage().rss;

    mf = runtime(join(directory, 'storage'), workerDirectory);
    await mf.ready;
    const call = async (path, body, token) => {
      const response = await mf.dispatchFetch(`https://jot-game.pages.dev${path}`, {
        method: body ? 'POST' : 'GET',
        headers: { 'CF-Connecting-IP': '192.0.2.99',
          ...(body ? { 'Content-Type': 'application/json' } : {}),
          ...(token ? { Authorization: `Bearer ${token}` } : {}) },
        ...(body ? { body: JSON.stringify(body) } : {}),
      });
      const value = await response.json();
      assert(response.ok, JSON.stringify(value));
      return value;
    };
    start = performance.now();
    const host = await call('/api/rooms', { requestId: randomUUID() });
    metric('httpColdRoomCreate', [performance.now() - start]);
    const path = `/api/rooms/${host.roomId}`;
    const guest = await call(`${path}/join`, { requestId: randomUUID(), inviteToken: host.inviteToken });
    const storage = await mf.unsafeGetDurableObjectStorage('jot-multiplayer', 'GameRoom', { name: host.roomId });
    const rows = await storage.exec('SELECT metadata FROM room WHERE singleton = 1');
    const meta = JSON.parse(rows[0].metadata); meta.revision++;
    await storage.exec('UPDATE room SET save = ?, metadata = ? WHERE singleton = 1', JSON.stringify(saved), JSON.stringify(meta));
    await mf.unsafeEvictDurableObject('jot-multiplayer', 'GameRoom', { name: host.roomId });
    const firstView = await call(path, undefined, host.playerToken);
    assert.deepEqual(firstView.state, state);
    const readSamples = [];
    for (let i = 0; i < count; i++) {
      start = performance.now();
      const result = await call(path, undefined, i % 2 ? guest.playerToken : host.playerToken);
      assert.equal(result.state.viewerPlayer, i % 2);
      readSamples.push(performance.now() - start);
    }
    metric('workerdAlternatingPrivateReads', readSamples);
    report.nodeProcessRssAfterHttpBytes = process.memoryUsage().rss;
  } finally {
    await mf?.dispose();
    delete globalThis.__jotBenchmarkWasm;
    delete globalThis.__jotBenchmarkDictionary;
    await rm(directory, { recursive: true, force: true });
  }
}
if (args.has('output')) await writeFile(String(args.get('output')), JSON.stringify(report, null, 2) + '\n');
console.log(JSON.stringify(report, null, 2));
