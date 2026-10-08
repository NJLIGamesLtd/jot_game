import { readdirSync } from 'node:fs';
import { resolve } from 'node:path';
import { Miniflare, convertV4MiniflareOptions } from 'miniflare';
import { collectCoverage } from './coverage-collect.mjs';

export function runtime(persistence) {
  const covered = process.env.JOT_COVERAGE === '1';
  const directory = covered ? '.coverage-build' : 'dist';
  const files = covered ? ['index.js', ...readdirSync(`${directory}/generated`).map((file) => `generated/${file}`)]
    : ['index.js', ...readdirSync(directory).filter((file) => /\.(wasm|txt)$/.test(file))];
  const mf = new Miniflare(convertV4MiniflareOptions({
    name: 'jot-multiplayer', modulesRoot: resolve(directory),
    modules: files.map((file) => ({
      type: file.endsWith('.wasm') ? 'CompiledWasm' : file.endsWith('.txt') ? 'Text' : 'ESModule',
      path: resolve(directory, file),
    })),
    compatibilityDate: '2026-10-07', compatibilityFlags: ['nodejs_compat'],
    bindings: { PUBLIC_ORIGIN: 'https://jot-game.pages.dev' },
    durableObjects: { ROOMS: { className: 'GameRoom', useSQLite: true } },
    ratelimits: {
      INGRESS_LIMITER: { namespace_id: '10871003', simple: { limit: 600, period: 60 } },
      CREATE_LIMITER: { namespace_id: '10871001', simple: { limit: 10, period: 60 } },
      API_LIMITER: { namespace_id: '10871002', simple: { limit: 120, period: 60 } },
    },
    resourcePersistencePath: persistence, telemetry: { enabled: false }, unsafeInspectDurableObjects: true,
    ...(covered ? { inspectorPort: 0 } : {}),
  }));
  if (covered) {
    const dispose = mf.dispose.bind(mf);
    let disposed = false;
    mf.dispose = async () => { if (disposed) return; disposed = true; try { await collectCoverage(mf); } finally { await dispose(); } };
  }
  return mf;
}
