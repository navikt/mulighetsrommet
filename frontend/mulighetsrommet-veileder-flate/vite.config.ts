import { defineConfig } from 'vite';
import react from '@vitejs/plugin-react';
import { visualizer } from 'rollup-plugin-visualizer';

export default defineConfig({
  plugins: [
    react(),
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
