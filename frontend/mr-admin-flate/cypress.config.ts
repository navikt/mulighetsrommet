import { defineConfig } from "cypress";

export default defineConfig({
  viewportHeight: 1300,
  viewportWidth: 1800,
  requestTimeout: 10000,
  defaultCommandTimeout: 10000,
  video: false,
  retries: {
    runMode: 2,
  },
  e2e: {
    setupNodeEvents(on, config) {
      return require("./cypress/plugins/index.js")(on, config);
    },
    baseUrl: "http://localhost:5173",
  },
});
