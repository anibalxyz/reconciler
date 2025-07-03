import { defineConfig, loadEnv } from "vite";
import path from "path";

const envDir = path.resolve(__dirname, ".");
const envFile = loadEnv(process.env.NODE_ENV!, envDir, "");

export default defineConfig({
  server: {
    proxy: {
      "/api": {
        target: envFile.API_URL,
        changeOrigin: true,
        rewrite: (path) => path.replace(/^\/api/, ""),
      },
    },
  },
});
