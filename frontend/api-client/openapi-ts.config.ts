import { defineConfig } from "@hey-api/openapi-ts";

export default defineConfig({
  input: "../../mulighetsrommet-api/src/main/resources/web/openapi.yaml",
  output: "./build",
  client: "legacy/fetch",
  plugins: [
    "@hey-api/schemas",
    {
      name: "@hey-api/services",
      asClass: true,
    },
    {
      name: "@hey-api/types",
      enums: "typescript",
    },
  ],
});
