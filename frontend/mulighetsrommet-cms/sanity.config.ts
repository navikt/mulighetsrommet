import { createConfig, createPlugin } from "sanity";
import { deskTool } from "sanity/desk";
import { schemaTypes } from "./schemas";
import { structure, defaultDocumentNode } from "./desk/structure";

const mulighetsrommetConfig = createPlugin({
  name: "mulighetsrommet",
  plugins: [
    deskTool({
      defaultDocumentNode,
      structure,
    }),
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
