import type { Config } from "tailwindcss";

import akselTailwind from "@navikt/ds-tailwind";

export default {
  content: ["./src/**/*.{js,jsx,ts,tsx}", "./src/apps/**/index.html"],
  theme: {
    extend: {},
  },
  plugins: [require("@tailwindcss/typography")],
  presets: [akselTailwind],
} satisfies Config;
