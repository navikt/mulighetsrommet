import { defineConfig } from "vitest/config";
import react from "@vitejs/plugin-react";
import terser from "@rollup/plugin-terser";

// https://vitejs.dev/config/
export default defineConfig({
  server: {
    port: 5173,
    host: "127.0.0.1",
    open: true,
  },
  plugins: [react(), terser()],
  base: process.env.VITE_BASE || "/",
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
