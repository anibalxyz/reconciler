import { mergeConfig } from 'vite';
import { getEnv } from '../common/helpers/env.ts';
import baseConfig from '../vite.config.ts';
import react from '@vitejs/plugin-react-swc';
import tsConfigPaths from 'vite-tsconfig-paths';
import { buildUrl } from '../common/helpers/buildUrl.ts';

const env = getEnv();
const PUBLIC_SITE_URL = buildUrl(env.PUBLIC_SITE_HOST, env.PUBLIC_SITE_PORT) ?? 'http://localhost:5174';

console.log('PUBLIC_SITE_URL: ', PUBLIC_SITE_URL);

export default mergeConfig(baseConfig, {
  plugins: [react(), tsConfigPaths()],
  root: __dirname,
  base: '/dashboard',
  envDir: '..',
  server: {
    port: parseInt(env.DASHBOARD_PORT),
    proxy: {
      '^/(?!dashboard)': {
        // use a default value because it's evaluated in prod even if it's not used.
        target: PUBLIC_SITE_URL,
      },
    },
  },
});
