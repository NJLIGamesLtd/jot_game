import { mkdir, readFile, rm, copyFile } from 'node:fs/promises';
import { dirname, resolve, basename } from 'node:path';
import { build } from 'esbuild';
import { createInstrumenter } from 'istanbul-lib-instrument';
import { productionSources } from './coverage-scope.mjs';

const directory = resolve('.coverage-build');
await rm(directory, { recursive: true, force: true });
await mkdir(resolve(directory, 'generated'), { recursive: true });
const authored = new Set(await productionSources());
await build({
  entryPoints: ['test/coverage-worker.mjs'], outfile: resolve(directory, 'index.js'),
  bundle: true, format: 'esm', platform: 'neutral', target: 'es2022',
  external: ['cloudflare:workers'],
  plugins: [{
    name: 'instrument-authored-worker-code',
    setup(builder) {
      builder.onResolve({ filter: /\.(wasm|txt)$/ }, async ({ path, importer }) => {
        const original = resolve(dirname(importer), path);
        await copyFile(original, resolve(directory, 'generated', basename(original)));
        return { path: `./generated/${basename(original)}`, external: true };
      });
      builder.onResolve({ filter: /generated\/jot_server_core\.mjs$/ }, ({ path, importer }) => {
        if (basename(importer) === 'server_core.mjs') return { path: resolve('test/fault-factory.mjs') };
      });
      builder.onLoad({ filter: /\.(ts|mjs|js)$/ }, async ({ path }) => {
        if (!authored.has(path)) return;
        const source = await readFile(path, 'utf8');
        const instrumenter = createInstrumenter({
          esModules: true, parserPlugins: ['typescript'], produceSourceMap: true,
          coverageGlobalScope: 'globalThis', coverageGlobalScopeFunc: false,
        });
        return { contents: instrumenter.instrumentSync(source, path), loader: path.endsWith('.ts') ? 'ts' : 'js' };
      });
    },
  }],
});
