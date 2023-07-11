import { defineConfig } from 'cypress';
import { plugin } from './cypress/plugins/index';

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
    //@ts-ignore
    setupNodeEvents(on, config) {
      return plugin(on, config);
    },
    baseUrl: 'http://localhost:3000',
  },
});
