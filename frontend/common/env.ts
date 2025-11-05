import { loadEnv } from 'vite';
import path from 'path';

const modeMap: Record<string, string> = {
  development: 'dev',
  production: 'prod',
  test: 'test',
};

export function getEnv(dirname: string, baseDirUp = '.') {
  const mode = modeMap[process.env.NODE_ENV ?? ''] ?? '';
  const envDir = path.resolve(dirname, baseDirUp);

  return loadEnv(mode, envDir, '');
}
