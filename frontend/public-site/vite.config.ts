import { mergeConfig, UserConfig, loadEnv } from "vite";
import baseConfig from "../vite.config.ts";
import path from "path";

const envDir = path.resolve(__dirname, "..");
const envFile = loadEnv(process.env.NODE_ENV!, envDir, "");

export default mergeConfig<UserConfig, UserConfig>(baseConfig, {
  root: __dirname,
  base: "/",
  envDir: "..",
  resolve: {
    alias: {
      "@": path.resolve(__dirname, "./src"),
      "@common": path.resolve(__dirname, "./src/common"),
    },
  },
  build: {
    rollupOptions: {
      input: {
        index: path.resolve(__dirname, "index.html"),
        about: path.resolve(__dirname, "about.html"),
      },
    },
  },
  server: {
    port: 5173,
    proxy: {
      "/dashboard": {
        target: envFile.DASHBOARD_URL,
      },
    },
  },
});
