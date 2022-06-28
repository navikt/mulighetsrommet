const { defineConfig } = require('cypress');

module.exports = defineConfig({
  e2e: {
    baseUrl: 'http://localhost:3000',
    viewportHeight: 1300,
    viewportWidth: 1800,
    requestTimeout: 10000,
    defaultCommandTimeout: 10000,
    retries: {
      runMode: 2,
    },
    component: {
      componentFolder: 'src',
      testFiles: '.*/__tests__/.*spec.tsx',
    },
    video: false,
    videoUploadOnPasses: false,
  },
});
