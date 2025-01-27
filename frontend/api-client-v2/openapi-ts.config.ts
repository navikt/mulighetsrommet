import { defineConfig } from "@hey-api/openapi-ts";

export default defineConfig({
  input: "../../mulighetsrommet-api/src/main/resources/web/openapi.yaml",
  output: "./build",
  client: "@hey-api/client-fetch",
  plugins: [
    "@hey-api/schemas",
    {
      name: "@hey-api/sdk",
      asClass: true,
      throwOnError: true,
    },
    {
      name: "@hey-api/typescript",
      enums: "typescript",
    },
  ],
});
