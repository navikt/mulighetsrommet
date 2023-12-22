import { defineConfig } from "vite";
import react from "@vitejs/plugin-react";
import { visualizer } from "rollup-plugin-visualizer";

export default defineConfig({
  server: {
    port: 3000,
    host: "127.0.0.1",
    open: true,
  },
  plugins: [
    react(),
    visualizer({
      filename: "bundle-stats.html",
    }),
  ],
  build: {
    manifest: "asset-manifest.json",
    chunkSizeWarningLimit: 1400,
    sourcemap: true,
  },
});
