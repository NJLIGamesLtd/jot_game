import { readdir } from 'node:fs/promises';
import { resolve, join } from 'node:path';

export async function productionSources() {
  const sources = [resolve('server_core.mjs')];
  async function visit(directory) {
    for (const entry of await readdir(directory, { withFileTypes: true })) {
      const path = join(directory, entry.name);
      if (entry.isDirectory()) await visit(path);
      else if (/\.(ts|js|mjs)$/.test(entry.name) && !entry.name.endsWith('.d.ts')) sources.push(path);
    }
  }
  await visit(resolve('src'));
  return sources.sort();
}
