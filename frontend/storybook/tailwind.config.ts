import type { Config } from "tailwindcss";

export default {
  content: ["../mr-admin-flate/src/**/*.{js,jsx,ts,tsx}", "./index.html"],
  theme: {
    extend: {},
  },
  plugins: [require("@tailwindcss/typography")],
  presets: [require("@navikt/ds-tailwind")],
} satisfies Config;
