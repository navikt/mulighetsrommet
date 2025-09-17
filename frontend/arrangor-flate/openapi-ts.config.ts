import { defineConfig } from "@hey-api/openapi-ts";

export default defineConfig({
  input: "./openapi.yaml",
  output: "api-client",
  parser: {
    transforms: {
      enums: "root",
    },
  },
  plugins: [
    "@hey-api/schemas",
    { name: "@hey-api/client-fetch", exportFromIndex: true, throwOnError: false },
    {
      name: "@hey-api/sdk",
      asClass: true,
      classNameBuilder: (name: string) => `${name}Service`,
    },
    {
      name: "@hey-api/typescript",
      enums: "typescript",
    },
  ],
});
