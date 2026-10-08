import { rm, mkdir } from 'node:fs/promises';
import { resolve } from 'node:path';
import { spawn } from 'node:child_process';

const directory = resolve('../coverage/multiplayer');
await rm(directory, { recursive: true, force: true });
await mkdir(resolve(directory, 'raw'), { recursive: true });
function run(args, extra = {}) {
  return new Promise((accept, reject) => {
    const child = spawn(process.execPath, args, { stdio: 'inherit', env: { ...process.env, ...extra } });
    child.once('error', reject);
    child.once('exit', (code) => code === 0 ? accept() : reject(new Error(`Coverage command exited ${code}: ${args.join(' ')}`)));
  });
}
await run(['test/coverage-build.mjs']);
await run(['--test', 'test/coverage-paths.test.mjs', 'test/rooms.test.mjs'], { JOT_COVERAGE: '1' });
await run(['test/coverage-report.mjs'], { JOT_ENFORCE_COVERAGE: '1' });
