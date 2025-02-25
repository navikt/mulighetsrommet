import type { Config } from "tailwindcss";

export default {
  content: ["./src/**/*.{js,jsx,ts,tsx}", "./src/apps/**/index.html"],
  theme: {
    extend: {},
  },
  plugins: [],
  presets: [require("@navikt/ds-tailwind")],
} satisfies Config;
