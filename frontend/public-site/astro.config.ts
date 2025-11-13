import baseConfig from '../vite.config.ts';
import { mergeConfig, UserConfig } from 'vite';
import { defineConfig } from 'astro/config';
import { getEnv } from '../common/env.ts';

const env = getEnv();

export default defineConfig({
  server: {
    port: 5173,
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
