/// <reference types="vitest" />
import { defineConfig } from "vite";
import react from "@vitejs/plugin-react";
import { rollupImportMapPlugin } from "rollup-plugin-import-map";
import importmap from "./importmap.json" assert { type: "json" };
import terser from "@rollup/plugin-terser";
import EnvironmentPlugin from "vite-plugin-environment";

// https://vitejs.dev/config/
export default defineConfig({
  server: {
    port: 5173,
    host: "127.0.0.1",
    open: true,
  },
  plugins: [
    react(),
    {
      ...rollupImportMapPlugin([importmap]),
      enforce: "pre",
      apply: "build",
    },
    terser(),
    EnvironmentPlugin({ NODE_ENV: process.env.NODE_ENV || "development" }),
  ],
  base: process.env.VITE_BASE || "/",
  build: {
    manifest: "asset-manifest.json",
    chunkSizeWarningLimit: 1400,
    sourcemap: true,
  },
  test: {
    environment: "jsdom",
    include: ["./src/**/*.test.?(c|m)[jt]s?(x)"],
  },
});
