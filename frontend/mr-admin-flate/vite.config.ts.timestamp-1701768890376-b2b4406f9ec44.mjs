// vite.config.ts
import { defineConfig } from "file:///Users/fpi/work/nav/mulighetsrommet/node_modules/vite/dist/node/index.js";
import react from "file:///Users/fpi/work/nav/mulighetsrommet/node_modules/@vitejs/plugin-react/dist/index.mjs";
import { rollupImportMapPlugin } from "file:///Users/fpi/work/nav/mulighetsrommet/node_modules/rollup-plugin-import-map/lib/plugin.js";

// importmap.json
var importmap_default = {
  imports: {
    react: "https://www.nav.no/tms-min-side-assets/react/18/esm/index.js",
    "react-dom": "https://www.nav.no/tms-min-side-assets/react-dom/18/esm/index.js"
  }
};

// vite.config.ts
import terser from "file:///Users/fpi/work/nav/mulighetsrommet/node_modules/@rollup/plugin-terser/dist/es/index.js";
import EnvironmentPlugin from "file:///Users/fpi/work/nav/mulighetsrommet/node_modules/vite-plugin-environment/dist/index.js";
var vite_config_default = defineConfig({
  server: {
    port: 5173,
    host: "127.0.0.1",
    open: true
  },
  plugins: [
    react(),
    {
      ...rollupImportMapPlugin([importmap_default]),
      enforce: "pre",
      apply: "build"
    },
    terser(),
    EnvironmentPlugin({ NODE_ENV: process.env.NODE_ENV || "development" })
  ],
  base: process.env.VITE_BASE || "/",
  build: {
    manifest: "asset-manifest.json",
    chunkSizeWarningLimit: 1400,
    sourcemap: true
  },
  test: {
    environment: "jsdom",
    include: ["./src/**/*.test.?(c|m)[jt]s?(x)"]
  }
});
export {
  vite_config_default as default
};
//# sourceMappingURL=data:application/json;base64,ewogICJ2ZXJzaW9uIjogMywKICAic291cmNlcyI6IFsidml0ZS5jb25maWcudHMiLCAiaW1wb3J0bWFwLmpzb24iXSwKICAic291cmNlc0NvbnRlbnQiOiBbImNvbnN0IF9fdml0ZV9pbmplY3RlZF9vcmlnaW5hbF9kaXJuYW1lID0gXCIvVXNlcnMvZnBpL3dvcmsvbmF2L211bGlnaGV0c3JvbW1ldC9mcm9udGVuZC9tci1hZG1pbi1mbGF0ZVwiO2NvbnN0IF9fdml0ZV9pbmplY3RlZF9vcmlnaW5hbF9maWxlbmFtZSA9IFwiL1VzZXJzL2ZwaS93b3JrL25hdi9tdWxpZ2hldHNyb21tZXQvZnJvbnRlbmQvbXItYWRtaW4tZmxhdGUvdml0ZS5jb25maWcudHNcIjtjb25zdCBfX3ZpdGVfaW5qZWN0ZWRfb3JpZ2luYWxfaW1wb3J0X21ldGFfdXJsID0gXCJmaWxlOi8vL1VzZXJzL2ZwaS93b3JrL25hdi9tdWxpZ2hldHNyb21tZXQvZnJvbnRlbmQvbXItYWRtaW4tZmxhdGUvdml0ZS5jb25maWcudHNcIjsvLy8gPHJlZmVyZW5jZSB0eXBlcz1cInZpdGVzdFwiIC8+XG5pbXBvcnQgeyBkZWZpbmVDb25maWcgfSBmcm9tIFwidml0ZVwiO1xuaW1wb3J0IHJlYWN0IGZyb20gXCJAdml0ZWpzL3BsdWdpbi1yZWFjdFwiO1xuaW1wb3J0IHsgcm9sbHVwSW1wb3J0TWFwUGx1Z2luIH0gZnJvbSBcInJvbGx1cC1wbHVnaW4taW1wb3J0LW1hcFwiO1xuaW1wb3J0IGltcG9ydG1hcCBmcm9tIFwiLi9pbXBvcnRtYXAuanNvblwiIGFzc2VydCB7IHR5cGU6IFwianNvblwiIH07XG5pbXBvcnQgdGVyc2VyIGZyb20gXCJAcm9sbHVwL3BsdWdpbi10ZXJzZXJcIjtcbmltcG9ydCBFbnZpcm9ubWVudFBsdWdpbiBmcm9tIFwidml0ZS1wbHVnaW4tZW52aXJvbm1lbnRcIjtcblxuLy8gaHR0cHM6Ly92aXRlanMuZGV2L2NvbmZpZy9cbmV4cG9ydCBkZWZhdWx0IGRlZmluZUNvbmZpZyh7XG4gIHNlcnZlcjoge1xuICAgIHBvcnQ6IDUxNzMsXG4gICAgaG9zdDogXCIxMjcuMC4wLjFcIixcbiAgICBvcGVuOiB0cnVlLFxuICB9LFxuICBwbHVnaW5zOiBbXG4gICAgcmVhY3QoKSxcbiAgICB7XG4gICAgICAuLi5yb2xsdXBJbXBvcnRNYXBQbHVnaW4oW2ltcG9ydG1hcF0pLFxuICAgICAgZW5mb3JjZTogXCJwcmVcIixcbiAgICAgIGFwcGx5OiBcImJ1aWxkXCIsXG4gICAgfSxcbiAgICB0ZXJzZXIoKSxcbiAgICBFbnZpcm9ubWVudFBsdWdpbih7IE5PREVfRU5WOiBwcm9jZXNzLmVudi5OT0RFX0VOViB8fCBcImRldmVsb3BtZW50XCIgfSksXG4gIF0sXG4gIGJhc2U6IHByb2Nlc3MuZW52LlZJVEVfQkFTRSB8fCBcIi9cIixcbiAgYnVpbGQ6IHtcbiAgICBtYW5pZmVzdDogXCJhc3NldC1tYW5pZmVzdC5qc29uXCIsXG4gICAgY2h1bmtTaXplV2FybmluZ0xpbWl0OiAxNDAwLFxuICAgIHNvdXJjZW1hcDogdHJ1ZSxcbiAgfSxcbiAgdGVzdDoge1xuICAgIGVudmlyb25tZW50OiBcImpzZG9tXCIsXG4gICAgaW5jbHVkZTogW1wiLi9zcmMvKiovKi50ZXN0Lj8oY3xtKVtqdF1zPyh4KVwiXSxcbiAgfSxcbn0pO1xuIiwgIntcbiAgXCJpbXBvcnRzXCI6IHtcbiAgICBcInJlYWN0XCI6IFwiaHR0cHM6Ly93d3cubmF2Lm5vL3Rtcy1taW4tc2lkZS1hc3NldHMvcmVhY3QvMTgvZXNtL2luZGV4LmpzXCIsXG4gICAgXCJyZWFjdC1kb21cIjogXCJodHRwczovL3d3dy5uYXYubm8vdG1zLW1pbi1zaWRlLWFzc2V0cy9yZWFjdC1kb20vMTgvZXNtL2luZGV4LmpzXCJcbiAgfVxufVxuIl0sCiAgIm1hcHBpbmdzIjogIjtBQUNBLFNBQVMsb0JBQW9CO0FBQzdCLE9BQU8sV0FBVztBQUNsQixTQUFTLDZCQUE2Qjs7O0FDSHRDO0FBQUEsRUFDRSxTQUFXO0FBQUEsSUFDVCxPQUFTO0FBQUEsSUFDVCxhQUFhO0FBQUEsRUFDZjtBQUNGOzs7QURBQSxPQUFPLFlBQVk7QUFDbkIsT0FBTyx1QkFBdUI7QUFHOUIsSUFBTyxzQkFBUSxhQUFhO0FBQUEsRUFDMUIsUUFBUTtBQUFBLElBQ04sTUFBTTtBQUFBLElBQ04sTUFBTTtBQUFBLElBQ04sTUFBTTtBQUFBLEVBQ1I7QUFBQSxFQUNBLFNBQVM7QUFBQSxJQUNQLE1BQU07QUFBQSxJQUNOO0FBQUEsTUFDRSxHQUFHLHNCQUFzQixDQUFDLGlCQUFTLENBQUM7QUFBQSxNQUNwQyxTQUFTO0FBQUEsTUFDVCxPQUFPO0FBQUEsSUFDVDtBQUFBLElBQ0EsT0FBTztBQUFBLElBQ1Asa0JBQWtCLEVBQUUsVUFBVSxRQUFRLElBQUksWUFBWSxjQUFjLENBQUM7QUFBQSxFQUN2RTtBQUFBLEVBQ0EsTUFBTSxRQUFRLElBQUksYUFBYTtBQUFBLEVBQy9CLE9BQU87QUFBQSxJQUNMLFVBQVU7QUFBQSxJQUNWLHVCQUF1QjtBQUFBLElBQ3ZCLFdBQVc7QUFBQSxFQUNiO0FBQUEsRUFDQSxNQUFNO0FBQUEsSUFDSixhQUFhO0FBQUEsSUFDYixTQUFTLENBQUMsaUNBQWlDO0FBQUEsRUFDN0M7QUFDRixDQUFDOyIsCiAgIm5hbWVzIjogW10KfQo=
