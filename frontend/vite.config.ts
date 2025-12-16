import { defineConfig, Plugin } from 'vite';
import { getEnv } from './common/helpers/getEnv.ts';
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

// Note: this aims to be just a temporal patch, until the use of npm swagger-ui
//       Generated with Claude
const swaggerPatchPlugin = (): Plugin => ({
  name: 'swagger-credentials-patch',
  configureServer(server) {
    server.middlewares.use(async (req, res, next) => {
      if (req.url === '/swagger') {
        try {
          const backendUrl = `${API_URL}/swagger`;
          const response = await fetch(backendUrl);
          let html = await response.text();

          html = html.replace(
            '</body>',
            `<script>
              (function() {
                const originalFetch = window.fetch;
                window.fetch = function(...args) {
                  const options = args[1] || {};
                  options.credentials = 'include';
                  args[1] = options;
                  return originalFetch.apply(this, args);
                };
                console.log('✅ Swagger patched via Vite');
              })();
            </script>
            </body>`,
          );

          res.setHeader('Content-Type', 'text/html; charset=utf-8');
          res.end(html);
          return;
        } catch (error) {
          console.error('Error proxying /swagger:', error);
        }
      }
      next();
    });
  },
});

export default defineConfig({
  plugins: [tailwindcss(), redirectApiDocsPlugin(), swaggerPatchPlugin()],
  server: {
    proxy: {
      '^/api($|/.*)': {
        target: API_URL,
        changeOrigin: true,
      },
      '^/openapi$': {
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
