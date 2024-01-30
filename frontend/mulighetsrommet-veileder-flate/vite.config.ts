import "dotenv/config";
import { resolve } from "path";
import { defineConfig } from "vite";
import react from "@vitejs/plugin-react";
import { shadowStyle } from "vite-plugin-shadow-style";
import { visualizer } from "rollup-plugin-visualizer";

const config = {
  DEMO: {
    root: resolve(__dirname, "src/apps/demo"),
  },
  MODIA: {
    root: resolve(__dirname, "src/apps/modia"),
  },
  NAV: {
    root: resolve(__dirname, "src/apps/nav"),
  },
  PREVIEW: {
    root: resolve(__dirname, "src/apps/preview"),
  },
} as const;

const APP = (process.env.APP ?? "DEMO") as keyof typeof config;

const appConfig = config[APP];

if (!appConfig) {
  throw Error(`The APP environment variable must be set to one of '${Object.keys(config)}'`);
} else {
  console.log(`Using config for app '${APP}': ${JSON.stringify(appConfig, null, 2)}`);
}

export default defineConfig({
  root: appConfig.root,
  publicDir: resolve(__dirname, "public"),
  plugins: [
    tsconfigPaths(),
    react(),
    visualizer({
      filename: "bundle-stats.html",
    }),
    shadowStyle(),
  ],
  build: {
    outDir: resolve(__dirname, "dist"), //appConfig.outDir,
    emptyOutDir: true,
    rollupOptions: {
      input: {
        main: resolve(appConfig.root, "index.html"),
      },
    },
    manifest: "asset-manifest.json",
    chunkSizeWarningLimit: 1400,
    sourcemap: true,
  },
  server: {
    port: 3000,
    host: "127.0.0.1",
    open: true,
  },
});
