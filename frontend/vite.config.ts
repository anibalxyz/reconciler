import { defineConfig } from 'vite';
import { getEnv } from './common/env.ts';
import tailwindcss from '@tailwindcss/vite';

const env = getEnv();

export default defineConfig({
  plugins: [tailwindcss()],
  server: {
    proxy: {
      '/api': {
        target: env.API_URL,
        changeOrigin: true,
        rewrite: (path) => path.replace(/^\/api/, ''),
        configure: (proxyServer) => {
          proxyServer.on('proxyRes', (proxyRes) => {
            const location = proxyRes.headers.location;
            if (location && location.startsWith('/')) {
              proxyRes.headers.location = '/api' + location;
            }
          });
        },
      },
      // NOTE: may fail if frontend uses these paths
      '^/(swagger|webjars|openapi)': {
        target: env.API_URL,
        changeOrigin: true,
      },
    },
  },
});
