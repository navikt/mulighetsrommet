import { createAuthStore, defineConfig } from "sanity";
import { structureTool } from "sanity/structure";
import { schemas } from "./schemas/schemas";
import { visionTool } from "@sanity/vision";
import { defaultDocumentNode, structure } from "./deskStructures/deskStrukture";

const PROJECT_ID = "xegcworx";
export const API_VERSION = "2024-04-23";

const createCommonConfig = (dataset: "production" | "test", basePath: string) => ({
  name: dataset,
  title: `Mulighetsrommet - ${dataset}`,
  projectId: PROJECT_ID,
  dataset,
  basePath,
  document: {
    unstable_comments: {
      // Comments enabled https://www.sanity.io/blog/introducing-comments
      enabled: true,
    },
    productionUrl: async (prev, context) => {
      const { document } = context;
      if (document._type !== "tiltaksgjennomforing") {
        return null;
      }

      const id = document._id?.replace("drafts.", "");
      const miljo = dataset === "test" ? "dev.nav.no" : "nav.no";
      return `https://nav-arbeidsmarkedstiltak.intern.${miljo}/preview/tiltak/${id}`;
    },
  },
  tools: (prev, { currentUser }) => {
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
    structureTool({
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
      // https://www.sanity.io/docs/migrating-custom-auth-providers#67b857c108e4
      {
        name: "saml",
        title: "NAV SSO",
        url: "https://api.sanity.io/v2021-10-01/auth/saml/login/f3270b37",
        logo: "/static/navlogo.svg",
      },
    ],
  }),
});

export default defineConfig([
  createCommonConfig("test", "/test"),
  createCommonConfig("production", "/prod"),
]);
