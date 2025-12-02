import { mergeConfig } from 'vite';
import { getEnv } from '../common/env.ts';
import baseConfig from '../vite.config.ts';
import react from '@vitejs/plugin-react-swc';
import tsConfigPaths from 'vite-tsconfig-paths';

const env = getEnv();
let PUBLIC_SITE_URL = undefined;
if (env.PUBLIC_SITE_HOST && env.PUBLIC_SITE_PORT) {
  PUBLIC_SITE_URL = 'http://' + env.PUBLIC_SITE_HOST + ':' + env.PUBLIC_SITE_PORT;
}
console.log('PUBLIC_SITE_URL: ', PUBLIC_SITE_URL || 'http://localhost:5174');
export default mergeConfig(baseConfig, {
  plugins: [react(), tsConfigPaths()],
  root: __dirname,
  base: '/dashboard',
  envDir: '..',
  server: {
    port: parseInt(env.DASHBOARD_PORT),
    proxy: {
      '^/(?!dashboard)': {
        // use a default value because it's analyzed in prod even if it's not used.
        target: PUBLIC_SITE_URL || 'http://localhost:5174',
      },
    },
  },
});
