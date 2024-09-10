import { defineCliConfig } from "sanity/cli";

export default defineCliConfig({
  api: {
    projectId: "xegcworx",
    dataset: "test",
  },
  vite: {
    build: {
      target: "esnext",
    },
  },
});
