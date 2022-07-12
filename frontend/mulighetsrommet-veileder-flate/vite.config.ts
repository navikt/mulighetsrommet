import { defineConfig } from 'vite';
import react from '@vitejs/plugin-react';

export default defineConfig({
  plugins: [react()],
  base: './',
  build: {
    manifest: 'asset-manifest.json',
    chunkSizeWarningLimit: 1400,
  },
});
