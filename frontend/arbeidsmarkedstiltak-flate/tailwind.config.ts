import type { Config } from "tailwindcss";
import dsTailwind from "@navikt/ds-tailwind";
import path from "path";

export default {
  content: [
    "./src/**/*.{js,jsx,ts,tsx}",
    "./src/apps/**/index.html",
    path.join(
      path.dirname(require.resolve("@mr/frontend-common/package.json")),
      "**/*.{js,jsx,ts,tsx}",
    ),
  ],
  theme: {
    extend: {},
  },
  plugins: [],
  presets: [dsTailwind],
} satisfies Config;
