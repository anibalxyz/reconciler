import baseConfig from '../vite.config.ts';
import { mergeConfig, UserConfig } from 'vite';
import { defineConfig } from 'astro/config';
import { getEnv } from '../common/env.ts';

const env = getEnv();
let DASHBOARD_URL = undefined;
if (env.DASHBOARD_HOST && env.DASHBOARD_PORT) {
  DASHBOARD_URL = 'http://' + env.DASHBOARD_HOST + ':' + env.DASHBOARD_PORT;
}
console.log('DASHBOARD_URL: ', DASHBOARD_URL || 'http://localhost:5175');
export default defineConfig({
  server: {
    // use a default value because it's analyzed in prod even if it's not used.
    port: parseInt(env.PUBLIC_SITE_PORT) || 5174,
  },
  vite: mergeConfig<UserConfig, UserConfig>(baseConfig, {
    server: {
      proxy: {
        '/dashboard': {
          // same as in server.port
          target: DASHBOARD_URL || 'http://localhost:5175',
        },
      },
    },
  }),
});
