import { mergeConfig, loadEnv } from "vite";
import baseConfig from "../vite.config.ts";
import react from "@vitejs/plugin-react";
import path from "path";

const envDir = path.resolve(__dirname, "..");
const envFile = loadEnv(process.env.NODE_ENV!, envDir, "");

export default mergeConfig(baseConfig, {
  plugins: [react()],
  root: __dirname,
  base: "/dashboard",
  envDir: "..",
  resolve: {
    alias: {
      "@": path.resolve(__dirname, "src"),
    },
  },
  server: {
    port: 5174,
    proxy: {
      "^/(?!dashboard)": {
        target: envFile.PUBLIC_SITE_URL,
      },
    },
  },
});
