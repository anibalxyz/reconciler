import { defineConfig, Plugin } from 'vite';
import { getEnv } from './common/helpers/getEnv.ts';
import tailwindcss from '@tailwindcss/vite';

const env = getEnv();
const API_URL = env.API_URL ?? 'http://localhost:4001';

console.log('API_URL: ', API_URL);

const redirectsPlugin = (): Plugin => ({
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
      if (req.url?.startsWith('/api/webjars')) {
        redirect(req.url.replace('/api/webjars', '/webjars'));
        return;
      }
      next();
    });
  },
});

export default defineConfig({
  plugins: [tailwindcss(), redirectsPlugin()],
  server: {
    proxy: {
      '^/api/health$': {
        target: API_URL,
        changeOrigin: true,
        rewrite: (path) => path.replace(/^\/api/, ''),
      },
      '^/api($|/.*)': {
        target: API_URL,
        changeOrigin: true,
      },

      '^/(openapi|swagger)$': {
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
