import {reactRouter} from "@react-router/dev/vite";
import {defineConfig} from "vitest/config";
import tsconfigPaths from "vite-tsconfig-paths";
import tailwindcss from "@tailwindcss/vite";

export default defineConfig({
  server: {
    port: 3000,
  },
  build: {
    manifest: true,
  },
  plugins: [reactRouter(), tsconfigPaths(), tailwindcss()],
  test: {
    environment: "jsdom",
    include: ["./app/**/*.test.?(c|m)[jt]s?(x)"],
  },
});
