import "dotenv/config";
import { resolve } from "path";
import { defineConfig } from "vite";
import { visualizer } from "rollup-plugin-visualizer";
import react from "@vitejs/plugin-react";
import { shadowStyle } from "vite-plugin-shadow-style";
import tsconfigPaths from "vite-tsconfig-paths";
import tailwindcss from "@tailwindcss/vite";

const config = {
  LOKAL: {
    root: resolve(__dirname, "src/apps/lokal"),
  },
  MODIA: {
    root: resolve(__dirname, "src/apps/modia"),
  },
  NAV: {
    root: resolve(__dirname, "src/apps/nav"),
  },
} as const;

const APP = (process.env.APP ?? "LOKAL") as keyof typeof config;

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
    shadowStyle({
      iife: true, // Isolate CSS to prevent conflicts
    }),
    tailwindcss(),
  ],
  css: {
    preprocessorOptions: {
      scss: {
        api: "modern-compiler",
      },
    },
  },
  resolve: {
    dedupe: await dedupeDependencies("@mr/frontend-common"),
  },
  build: {
    outDir: resolve(__dirname, "dist"),
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

async function dedupeDependencies(lib: string) {
  const projectPackageJson = (await import("./package.json", { with: { type: "json" } }))
    .default as { dependencies?: object };
  const projectDependencies = Object.keys(projectPackageJson.dependencies ?? {});

  const libPackageJson = (await import(`${lib}/package.json`, { with: { type: "json" } }))
    .default as { dependencies?: object };

  return Object.keys(libPackageJson.dependencies ?? {}).filter((p) =>
    projectDependencies.includes(p),
  );
}
