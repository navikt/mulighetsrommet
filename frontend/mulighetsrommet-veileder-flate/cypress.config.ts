import { defineConfig } from 'cypress';

export default defineConfig({
  viewportHeight: 1300,
  viewportWidth: 1800,
  requestTimeout: 10000,
  defaultCommandTimeout: 10000,
  retries: {
    //runMode: 2,
  },
  video: true,
  videoUploadOnPasses: true,
  e2e: {
    // We've imported your old cypress plugins here.
    // You may want to clean this up later by importing these.
    setupNodeEvents(on, config) {
      return require('./cypress/plugins/index.js')(on, config);
    },
    baseUrl: 'http://localhost:3000',
  },
  // component: {
  //   setupNodeEvents(on, config) {},
  //   specPattern: 'src/.*/__tests__/.*spec.tsx',
  // },
});
