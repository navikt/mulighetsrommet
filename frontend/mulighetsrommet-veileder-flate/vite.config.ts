import { defineConfig } from 'vite';
import reactRefresh from '@vitejs/plugin-react-refresh';

export default defineConfig({
  plugins: [reactRefresh()],
  build: {
    manifest: 'asset-manifest.json',
  },
  define: {
    // Polyfill the window.global object used by `@navikt/navspa`
    global: {},
  },
});
