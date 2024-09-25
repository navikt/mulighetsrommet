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
  css: {
    preprocessorOptions: {
      scss: {
        api: 'modern-compiler',
      },
    },
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
