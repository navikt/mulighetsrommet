import { createAuthStore, defineConfig } from "sanity";
import { deskTool } from "sanity/desk";
import { schemas } from "./schemas/schemas";
import { visionTool } from "@sanity/vision";
import { defaultDocumentNode, structure } from "./deskStructures/deskStrukture";
import { createClient } from "@sanity/client";

const PROJECT_ID = "xegcworx";
export const API_VERSION = "2021-10-21";

const createCommonConfig = (dataset: "production" | "test", basePath: string) => ({
  name: dataset,
  title: `Mulighetsrommet - ${dataset}`,
  projectId: PROJECT_ID,
  dataset,
  basePath,
  document: {
    unstable_comments: {
      enabled: true, // Comments enabled https://www.sanity.io/blog/introducing-comments?utm_source=slack&utm_medium=organic_social&utm_campaign=comments-announce&utm_content=
    },
    productionUrl: async (prev, context) => {
      const { document } = context;
      if (document._type !== "tiltaksgjennomforing") {
        return null;
      }

      const id = document._id?.replace("drafts.", "");
      const miljo = dataset === "test" ? "dev.nav.no" : "nav.no";
      return `https://mulighetsrommet-veileder-flate.intern.${miljo}/preview/tiltak/${id}`;
    },
  },
  tools: (prev, { currentUser }) => {
    // Check if the current user is an administrator or editor (Tom Stian og Marthe)
    const isAdmin = currentUser?.roles.some((role) =>
      ["administrator", "editor"].includes(role.name),
    );

    // Filter out the tools that should not be available to non-administrators
    const nonAdminDeskTools = prev.filter((tool) => !["visionTool"].includes(tool.name));

    // Return tools available to non-administrators
    if (!isAdmin) return nonAdminDeskTools;

    return prev;
  },
  plugins: [
    deskTool({
      structure: structure,
      defaultDocumentNode,
    }),
    visionTool({
      defaultApiVersion: API_VERSION,
      defaultDataset: "test",
      name: "visionTool",
    }),
  ],
  schema: {
    types: schemas,
  },
  auth: createAuthStore({
    dataset,
    projectId: PROJECT_ID,
    redirectOnSingle: true,
    mode: "replace",
    providers: [
      {
        name: "saml",
        title: "NAV SSO",
        url: "https://api.sanity.io/v2021-10-01/auth/saml/login/f3270b37",
        logo: "/static/navlogo.svg",
      },

      // https://www.sanity.io/docs/migrating-custom-auth-providers#67b857c108e4
      //Fjerner ubrukte login-løsninger

      // {
      //   name: "google",
      //   title: "Google",
      //   url: "https://api.sanity.io/v1/auth/login/google",
      // },
      // {
      //   name: "github",
      //   title: "GitHub",
      //   url: "https://api.sanity.io/v1/auth/login/github",
      // },
      // {
      //   name: "sanity",
      //   title: "E-mail / password",
      //   url: "https://api.sanity.io/v1/auth/login/sanity",
      // },
    ],
  }),
});

export const client = createClient({
  projectId: PROJECT_ID,
  dataset: "production",
  useCdn: false,
  apiVersion: API_VERSION,
});

const currentUser = await client.request({
  uri: "/users/me",
  withCredentials: true,
});

const isAdmin = Boolean(currentUser.roles?.find((role) => role.name === "administrator"));

export default isAdmin
  ? defineConfig([
      {
        ...createCommonConfig("production", "/prod"),
      },
      {
        ...createCommonConfig("test", "/test"),
      },
    ])
  : defineConfig([
      {
        ...createCommonConfig("production", "/prod"),
      },
    ]);
