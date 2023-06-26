import { defineConfig } from "vite";
import react from "@vitejs/plugin-react";
import { rollupImportMapPlugin } from "rollup-plugin-import-map";
import importmap from "./importmap.json" assert { type: "json" };

// https://vitejs.dev/config/
export default defineConfig({
  server: {
    port: 5173,
    host: "127.0.0.1",
    open: true,
  },
  define: {
    "process.env": {},
  },
  plugins: [
    react(),
    {
      ...rollupImportMapPlugin([{ importmap }]),
      enforce: "pre",
      apply: "build",
    },
  ],
  base: process.env.VITE_BASE || "/",
  build: {
    manifest: "asset-manifest.json",
    chunkSizeWarningLimit: 1400,
    sourcemap: true,
  },
});
