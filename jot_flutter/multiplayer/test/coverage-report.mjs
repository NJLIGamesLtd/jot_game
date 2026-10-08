import { readdir, readFile, writeFile } from 'node:fs/promises';
import { resolve } from 'node:path';
import coverageLibrary from 'istanbul-lib-coverage';
import reportLibrary from 'istanbul-lib-report';
import reports from 'istanbul-reports';
import { productionSources } from './coverage-scope.mjs';

const directory = resolve('../coverage/multiplayer');
const coverage = coverageLibrary.createCoverageMap({});
for (const file of await readdir(resolve(directory, 'raw'))) coverage.merge(JSON.parse(await readFile(resolve(directory, 'raw', file), 'utf8')));
const expected = await productionSources();
for (const file of expected) if (!coverage.files().includes(file)) throw new Error(`Missing Worker coverage: ${file}`);
const context = reportLibrary.createContext({ dir: directory, coverageMap: coverage });
for (const reporter of ['text', 'json-summary', 'json', 'lcovonly', 'html']) reports.create(reporter).execute(context);
await writeFile(resolve(directory, 'scope.json'), JSON.stringify({
  runtime: 'Cloudflare workerd', instrumentation: 'Istanbul inside Worker and Durable Objects',
  files: expected, generatedArtifacts: 'Generated Emscripten glue, compiled C++ WASM and dictionary are validated by actual-runtime integration tests; C++ source coverage is measured separately.',
}, null, 2));
if (process.env.JOT_ENFORCE_COVERAGE !== '0') {
  for (const file of expected) {
    const measured = coverage.fileCoverageFor(file).toSummary().toJSON();
    for (const metric of ['lines', 'functions', 'statements', 'branches']) {
      if (measured[metric].pct !== 100) {
        console.error(`Worker ${metric} coverage must be 100% in ${file}; got ${measured[metric].pct}%`);
        process.exitCode = 1;
      }
    }
  }
}
