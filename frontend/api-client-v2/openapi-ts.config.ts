import { defineConfig } from "@hey-api/openapi-ts";

export default defineConfig({
  input: "../../mulighetsrommet-api/src/main/resources/web/openapi.yaml",
  output: "./build",
  plugins: [
    "@hey-api/schemas",
    { name: "@hey-api/client-fetch", exportFromIndex: true, throwOnError: true },
    {
      name: "@hey-api/sdk",
      asClass: true,
    },
    {
      name: "@hey-api/typescript",
      enums: "typescript",
    },
  ],
});
