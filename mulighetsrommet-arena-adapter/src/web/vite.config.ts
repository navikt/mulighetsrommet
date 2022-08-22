import { defineConfig } from "vite";
import react from "@vitejs/plugin-react";

// https://vitejs.dev/config/
export default defineConfig({
  plugins: [react()],
  server: {
    proxy: {
      "^/topics": {
        target: "http://0.0.0.0:8084",
        changeOrigin: true,
      },
    },
  },
});
