import { defineConfig, loadEnv } from "vite";
import react from "@vitejs/plugin-react";

// https://vitejs.dev/config/
export default defineConfig(({ mode }) => {
  const env = loadEnv(mode, process.cwd());
  return {
    plugins: [react()],
    server: {
      proxy: {
        "^/mulighetsrommet-arena-adapter": {
          target: "http://0.0.0.0:8084",
          changeOrigin: true,
          rewrite: (path) => path.replace(/^\/mulighetsrommet-arena-adapter/, ""),
          headers: {
            Authorization: `Bearer ${env.VITE_AUTH_TOKEN}`,
          },
        },
        "^/mulighetsrommet-api": {
          target: "http://0.0.0.0:8080",
          changeOrigin: true,
          rewrite: (path) => path.replace(/^\/mulighetsrommet-api/, ""),
          headers: {
            Authorization: `Bearer ${env.VITE_AUTH_TOKEN}`,
          },
        },
      },
    },
  };
});
