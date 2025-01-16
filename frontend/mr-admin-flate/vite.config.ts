import { defineConfig } from "vitest/config";
import react from "@vitejs/plugin-react";
import terser from "@rollup/plugin-terser";
import tsconfigPaths from "vite-tsconfig-paths";

// https://vitejs.dev/config/
export default defineConfig({
  server: {
    port: 5173,
    host: "127.0.0.1",
    open: true,
  },
  plugins: [tsconfigPaths(), react(), terser()],
  base: process.env.VITE_BASE || "/",
  resolve: {
    dedupe: await dedupeDependencies("@mr/frontend-common"),
  },
  build: {
    manifest: "asset-manifest.json",
    chunkSizeWarningLimit: 1400,
    sourcemap: true,
  },
  test: {
    environment: "jsdom",
    include: ["./src/**/*.test.?(c|m)[jt]s?(x)"],
  },
});

async function dedupeDependencies(lib: string) {
  const projectPackageJson = (await import("./package.json", { with: { type: "json" } }))
    .default as { dependencies?: object };
  const projectDependencies = Object.keys(projectPackageJson.dependencies ?? {});

  const libPackageJson = (await import(`${lib}/package.json`, { with: { type: "json" } }))
    .default as { dependencies?: object };

  return Object.keys(libPackageJson.dependencies ?? {}).filter((p) =>
    projectDependencies.includes(p),
  );
}
