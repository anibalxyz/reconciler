import { mergeConfig } from "vite";
import { getEnv } from "../common/env.ts";
import baseConfig from "../vite.config.ts";
import react from "@vitejs/plugin-react";
import path from "path";

const env = getEnv(import.meta.url, "..");

export default mergeConfig(baseConfig, {
  plugins: [react()],
  root: __dirname,
  base: "/dashboard",
  envDir: "..",
  server: {
    port: 5174,
    proxy: {
      "^/(?!dashboard)": {
        target: env.PUBLIC_SITE_URL,
      },
    },
  },
});
