import { defineConfig } from 'cypress';
import { plugin } from './cypress/plugins';

export default defineConfig({
  viewportHeight: 1300,
  viewportWidth: 1800,
  requestTimeout: 10000,
  defaultCommandTimeout: 10000,
  video: false,
  retries: {
    runMode: 4,
  },
  e2e: {
    setupNodeEvents(on) {
      plugin(on);
    },
    baseUrl: 'http://localhost:3000',
  },
});
