import { mergeConfig } from 'vite';
import { getEnv } from '../common/env.ts';
import baseConfig from '../vite.config.ts';
import react from '@vitejs/plugin-react-swc';
import tsConfigPaths from 'vite-tsconfig-paths';

const env = getEnv();

export default mergeConfig(baseConfig, {
  plugins: [react(), tsConfigPaths()],
  root: __dirname,
  base: '/dashboard',
  envDir: '..',
  server: {
    port: parseInt(env.DASHBOARD_PORT),
    proxy: {
      '^/(?!dashboard)': {
        target: env.PUBLIC_SITE_URL,
      },
    },
  },
});
