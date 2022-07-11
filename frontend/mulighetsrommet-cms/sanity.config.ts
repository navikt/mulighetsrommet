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

    // auth: createAuthStore({
    //   projectId: "xegcworx",
    //   dataset: "production",
    //   redirectOnSingle: false,
    //   mode: "replace",
    //   providers: [
    //     {
    //       name: "saml",
    //       title: "NAV SSO",
    //       url: "https://api.sanity.io/v2021-10-01/auth/saml/login/f3270b37",
    //       logo: "/static/navlogo.svg",
    //     },
    //   ],
    // }),
  },
  {
    name: "dev",
    title: "Dev",

    projectId: "xegcworx",
    dataset: "dev",
    basePath: "/dev",

    plugins: [mulighetsrommetConfig()],

    // auth: createAuthStore({
    //   projectId: "xegcworx",
    //   dataset: "production",
    //   redirectOnSingle: false,
    //   mode: "replace",
    //   providers: [
    //     {
    //       name: "saml",
    //       title: "NAV SSO",
    //       url: "https://api.sanity.io/v2021-10-01/auth/saml/login/f3270b37",
    //       logo: "/static/navlogo.svg",
    //     },
    //   ],
    // }),
  },
]);
