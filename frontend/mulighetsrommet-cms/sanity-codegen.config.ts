import { SanityCodegenConfig } from "sanity-codegen";

const config: SanityCodegenConfig = {
  schemaPath: "./schemas/schema.js",
  outputPath: "../mulighetsrommet-veileder-flate/src/schema.ts",
  generateTypeName: (name) =>
    `Sanity${name.at(0).toUpperCase()}${name.substring(1)}`,
};

export default config;
