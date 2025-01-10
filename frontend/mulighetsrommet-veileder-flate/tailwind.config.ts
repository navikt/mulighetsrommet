import type { Config } from "tailwindcss";

export default {
  content: ["./src/**/*.{js,jsx,ts,tsx}", "./src/apps/**/index.html"],
  theme: {
    extend: {},
  },
  plugins: [require("@tailwindcss/typography")],
  presets: [require("@navikt/ds-tailwind")],
} satisfies Config;
