import { reactRouter } from "@react-router/dev/vite";
import { defineConfig } from "vitest/config";
import tailwindcss from "@tailwindcss/vite";

export default defineConfig({
  server: {
    port: 3000,
  },
  resolve: {
    tsconfigPaths: true,
  },
  plugins: [reactRouter(), tailwindcss()],
  test: {
    environment: "jsdom",
    include: ["./app/**/*.test.?(c|m)[jt]s?(x)"],
  },
});
