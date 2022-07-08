import { createConfig, createPlugin } from "sanity";
import { deskTool } from "sanity/desk";
import { schemaTypes } from "./schemas";

const mulighetsrommetConfig = createPlugin({
  name: "mulighetsrommet",
  plugins: [
    deskTool(),
  ],
  schema: {
    types: schemaTypes,
  },
});

export default createConfig([
  {
    name: "default",
    title: "Prod",

    projectId: "xegcworx",
    dataset: "production",
    basePath: "/prod",

    plugins: [mulighetsrommetConfig()],
  },
  {
    name: "dev",
    title: "Dev",

    projectId: "xegcworx",
    dataset: "dev",
    basePath: "/dev",

    plugins: [mulighetsrommetConfig()],
  },
]);
