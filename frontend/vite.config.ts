import { defineConfig, Plugin } from 'vite';
import { getEnv } from './common/helpers/env.ts';
import tailwindcss from '@tailwindcss/vite';

const env = getEnv();
const API_URL = env.API_URL ?? 'http://localhost:4001';

console.log('API_URL: ', API_URL);

const redirectApiDocsPlugin = (): Plugin => ({
  name: 'redirect-api-docs',
  configureServer(server) {
    server.middlewares.use((req, res, next) => {
      function redirect(location: string) {
        res.writeHead(301, { Location: location });
        res.end();
      }
      if (req.url === '/api/swagger') {
        redirect('/swagger');
        return;
      }
      if (req.url === '/api/openapi') {
        redirect('/openapi');
        return;
      }
      next();
    });
  },
});

export default defineConfig({
  plugins: [tailwindcss(), redirectApiDocsPlugin()],
  server: {
    proxy: {
      '^/api($|/.*)': {
        target: API_URL,
        changeOrigin: true,
      },
      '^/(swagger|openapi)$': {
        target: API_URL,
        changeOrigin: true,
      },
      // /webjars/* | /openapi* -> served directly from the backend in SwaggerUI
      '^/(webjars/|openapi)': {
        target: API_URL,
        changeOrigin: true,
      },
    },
  },
});
