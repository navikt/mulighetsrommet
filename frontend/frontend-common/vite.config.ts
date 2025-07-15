import react from '@vitejs/plugin-react';
import { defineConfig } from "vitest/config";

import tsconfigPaths from "vite-tsconfig-paths";

// https://vitejs.dev/config/
export default defineConfig({
  server: {
    port: 5173,
    host: "127.0.0.1",
    open: true,
  },
  plugins: [tsconfigPaths(), react()],
  base: process.env.VITE_BASE || "/",
  test: {
    environment: "jsdom",
    include: ["./**/*.test.?(c|m)[jt]s?(x)"],
    globals: true
  },
});
