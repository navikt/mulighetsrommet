import { defineConfig } from 'vite';
import react from '@vitejs/plugin-react';
import { visualizer } from 'rollup-plugin-visualizer';
import vitePluginRequire from 'vite-plugin-require';

export default defineConfig({
  server: {
    port: 3000,
  },
  plugins: [
    react(),
    vitePluginRequire(),
    visualizer({
      filename: 'bundle-stats.html',
    }),
  ],
  base: './',
  build: {
    manifest: 'asset-manifest.json',
    chunkSizeWarningLimit: 1400,
    sourcemap: true,
  },
});
