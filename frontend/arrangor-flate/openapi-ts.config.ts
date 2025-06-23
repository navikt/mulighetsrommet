import { defineConfig } from "@hey-api/openapi-ts";

export default defineConfig({
  input: "../../mulighetsrommet-api/src/main/resources/web/openapi.yaml",
  output: "api-client",
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
