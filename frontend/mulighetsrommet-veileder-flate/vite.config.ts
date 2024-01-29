import { resolve } from "path";
import { defineConfig } from "vite";
import react from "@vitejs/plugin-react";
import { visualizer } from "rollup-plugin-visualizer";
import "dotenv/config";

const config = {
  DEMO: {
    root: "apps/demo",
    publicDir: "../../public",
  },
  MODIA: {
    root: "apps/modia",
    publicDir: "../../public",
  },
  NAV: {
    root: "apps/nav",
    publicDir: "../../public",
  },
  PREVIEW: {
    root: "apps/preview",
    publicDir: "../../public",
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
  publicDir: appConfig.publicDir,
  plugins: [
    react(),
    visualizer({
      filename: "bundle-stats.html",
    }),
  ],
  resolve: {
    alias: [
      {
        // Sett opp et alias for kildekoden slik at imports fra `apps/<app>/index.html` fungerer i dev-modus
        find: "/src",
        replacement: resolve(__dirname, "src"),
      },
    ],
  },
  build: {
    rollupOptions: {
      input: {
        main: resolve(__dirname, `${appConfig.root}/index.html`),
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
