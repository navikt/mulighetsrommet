import type { Config } from "tailwindcss";
import dsTailwind from "@navikt/ds-tailwind";
import path from "path";

export default {
  content: [
    "./src/**/*.{js,jsx,ts,tsx}",
    path.join(
      path.dirname(require.resolve("@mr/frontend-common/package.json")),
      "**/*.{js,jsx,ts,tsx}",
    ),
  ],
  presets: [dsTailwind],
} satisfies Config;
