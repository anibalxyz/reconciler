import { loadEnv } from "vite";
import path from "path";
import { fileURLToPath } from "node:url";
import { dirname } from "node:path";

const modeMap: Record<string, string> = {
  development: "dev",
  production: "prod",
  test: "test",
};

export function getEnv(importMetaUrl: string, baseDirUp = ".") {
  const mode = modeMap[process.env.NODE_ENV ?? ""] ?? "";

  const __filename = fileURLToPath(importMetaUrl);
  const __dirname = dirname(__filename);
  const envDir = path.resolve(__dirname, baseDirUp);

  return loadEnv(mode, envDir, "");
}
