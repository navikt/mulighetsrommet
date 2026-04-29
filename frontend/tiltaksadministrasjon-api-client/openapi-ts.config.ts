import { defineConfig } from "@hey-api/openapi-ts";

export default defineConfig({
  input: "./openapi.yaml",
  output: "./build",
  parser: {
    transforms: {
      enums: "root",
    },
  },
  plugins: [
    { name: "@hey-api/client-fetch", exportFromIndex: true, throwOnError: true },
    {
      name: "@hey-api/sdk",
      operations: {
        strategy: "byTags",
        containerName: (name) => `${name}Service`,
      },
    },
    {
      name: "@hey-api/typescript",
      enums: "typescript",
    },
  ],
});
