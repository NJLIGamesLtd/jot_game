import { createGame as createCppGame } from '../server_core.mjs';

export type CoreResult = {
  ok: boolean;
  message: string;
  state: Record<string, unknown>;
  save?: unknown;
  hint?: unknown;
};
export type CoreGame = {
  execute(command: Record<string, unknown>): CoreResult;
  close(): void;
};

export async function createGame(seed: number): Promise<CoreGame> {
  return createCppGame(seed, undefined);
}
