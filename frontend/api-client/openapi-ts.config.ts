import { defineConfig } from "@hey-api/openapi-ts";

export default defineConfig({
  input: "../../mulighetsrommet-api/src/main/resources/web/openapi.yaml",
  output: "./build",
  client: "legacy/fetch",
  types: {
    enums: "typescript",
  },
  services: {
    asClass: true,
  },
});
