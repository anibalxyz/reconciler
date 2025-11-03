import { defineConfig } from "vite";
import { getEnv } from "./common/env.ts";

const env = getEnv(import.meta.url, "..");

export default defineConfig({
  server: {
    proxy: {
      "/api": {
        target: env.API_URL,
        changeOrigin: true,
        rewrite: (path) => path.replace(/^\/api/, ""),
      },
    },
  },
});
