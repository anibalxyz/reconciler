import { defineConfig } from 'vite';
import { getEnv } from './common/env.ts';
import tailwindcss from '@tailwindcss/vite';

const env = getEnv(import.meta.dirname, '..');

export default defineConfig({
  plugins: [tailwindcss()],
  server: {
    proxy: {
      '/api': {
        target: env.API_URL,
        changeOrigin: true,
        rewrite: (path) => path.replace(/^\/api/, ''),
      },
    },
  },
});
