import { defineConfig } from 'cypress';

export default defineConfig({
  viewportHeight: 1300,
  viewportWidth: 1800,
  requestTimeout: 10000,
  defaultCommandTimeout: 10000,
  retries: {
    runMode: 2,
  },
  e2e: {
    setupNodeEvents(on, config) {
      return require('./cypress/plugins/index.js')(on, config);
    },
    baseUrl: 'http://localhost:3000',
    videoUploadOnPasses: false,
  },
  // component: {
  //   setupNodeEvents(on, config) {},
  //   specPattern: 'src/.*/__tests__/.*spec.tsx',
  // },
});
