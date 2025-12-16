import baseConfig from '../vite.config.ts';
import { mergeConfig, UserConfig } from 'vite';
import { defineConfig } from 'astro/config';
import { getEnv } from '../common/helpers/getEnv.ts';
import { buildUrl } from '../common/helpers/buildUrl.ts';

const env = getEnv();
const DASHBOARD_URL = buildUrl(env.DASHBOARD_HOST, env.DASHBOARD_PORT) ?? 'http://localhost:5175';

console.log('DASHBOARD_URL: ', DASHBOARD_URL);

export default defineConfig({
  server: {
    // use a default value because it's evaluated in prod even if it's not used.
    port: parseInt(env.PUBLIC_SITE_PORT) || 5174,
  },
  vite: mergeConfig<UserConfig, UserConfig>(baseConfig, {
    server: {
      proxy: {
        '/dashboard': {
          // same as in server.port
          target: DASHBOARD_URL,
        },
      },
    },
  }),
});
