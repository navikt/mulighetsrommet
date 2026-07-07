import { defineConfig, loadEnv } from "vite";
import react from "@vitejs/plugin-react";

// https://vitejs.dev/config/
export default defineConfig(({ mode }) => {
  const env = loadEnv(mode, process.cwd());
  return {
    plugins: [react()],
    server: {
      proxy: {
        "^/api/mulighetsrommet-arena-adapter": {
          target: "http://0.0.0.0:8084",
          changeOrigin: true,
          rewrite: (path) => path.replace(/^\/api\/mulighetsrommet-arena-adapter/, ""),
          headers: {
            Authorization: `Bearer ${env.VITE_AUTH_TOKEN}`,
          },
        },
        "^/api/mulighetsrommet-api": {
          target: "http://0.0.0.0:8080",
          changeOrigin: true,
          rewrite: (path) => path.replace(/^\/api\/mulighetsrommet-api/, ""),
          headers: {
            Authorization: `Bearer ${env.VITE_AUTH_TOKEN}`,
          },
        },
        "^/api/tiltakshistorikk": {
          target: "http://0.0.0.0:8070",
          changeOrigin: true,
          rewrite: (path) => path.replace(/^\/api\/tiltakshistorikk/, ""),
          headers: {
            Authorization: `Bearer ${env.VITE_AUTH_TOKEN}`,
          },
        },
        "^/api/tiltaksokonomi": {
          target: "http://0.0.0.0:8074",
          changeOrigin: true,
          rewrite: (path) => path.replace(/^\/api\/tiltaksokonomi/, ""),
          headers: {
            Authorization: `Bearer ${env.VITE_AUTH_TOKEN}`,
          },
        },
      },
    },
  };
});
