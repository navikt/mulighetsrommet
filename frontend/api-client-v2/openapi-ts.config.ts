import { defineConfig } from "@hey-api/openapi-ts";

export default defineConfig({
  input: "../../mulighetsrommet-api/src/main/resources/web/openapi.yaml",
  output: "./build",
  parser: {
    transforms: {
      enums: "root",
    },
  },
  plugins: [
    "@hey-api/schemas",
    { name: "@hey-api/client-fetch", exportFromIndex: true, throwOnError: true },
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
