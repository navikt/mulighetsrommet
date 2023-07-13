import { defineConfig } from "vite";
import react from "@vitejs/plugin-react";

// https://vitejs.dev/config/
export default defineConfig({
  plugins: [react()],
  server: {
    proxy: {
      "^/mulighetsrommet-arena-adapter": {
        target: "http://0.0.0.0:8084",
        changeOrigin: true,
        rewrite: (path) => path.replace(/^\/mulighetsrommet-arena-adapter/, ""),
      },
      "^/mulighetsrommet-api": {
        target: "http://0.0.0.0:8080",
        changeOrigin: true,
        rewrite: (path) => path.replace(/^\/mulighetsrommet-api/, ""),
      },
    },
  },
});
