import baseConfig from '../vite.config.ts';
import { mergeConfig, UserConfig } from 'vite';
import { defineConfig } from 'astro/config';
import { getEnv } from '../common/env.ts';

const env = getEnv();

export default defineConfig({
  server: {
    // use a default value because it's analyzed in prod even if it's not used.
    port: parseInt(env.PUBLIC_SITE_PORT) || 5174,
  },
  vite: mergeConfig<UserConfig, UserConfig>(baseConfig, {
    server: {
      proxy: {
        '/dashboard': {
          target: env.DASHBOARD_URL,
        },
      },
    },
  }),
});
