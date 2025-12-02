import { defineConfig } from 'vite';
import { getEnv } from './common/helpers/env.ts';
import tailwindcss from '@tailwindcss/vite';

const env = getEnv();
const API_URL = env.API_URL ?? 'http://localhost:4001';

console.log('API_URL: ', API_URL);

export default defineConfig({
  plugins: [tailwindcss()],
  server: {
    proxy: {
      '/api': {
        target: API_URL,
        changeOrigin: true,
        rewrite: (path) => path.replace(/^\/api/, ''),
      },
      '^/(swagger|webjars|openapi)': {
        target: API_URL,
        changeOrigin: true,
      },
    },
  },
});
