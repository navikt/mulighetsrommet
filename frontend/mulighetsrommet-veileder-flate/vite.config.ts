import { defineConfig } from 'vite';
import reactRefresh from '@vitejs/plugin-react-refresh';

export default defineConfig({
  plugins: [reactRefresh()],
  base: './',
  build: {
    manifest: 'asset-manifest.json',
    chunkSizeWarningLimit: 1400,
  },
});
