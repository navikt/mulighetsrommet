import type { Config } from "tailwindcss";

export default {
  content: ["./src/**/*.{js,jsx,ts,tsx}", "./src/apps/**/index.html"],
  theme: {
    extend: {},
  },
  safelist: ["grid-cols-[0_40%_1fr_2%]"],
  plugins: [],
  presets: [require("@navikt/ds-tailwind")],
} satisfies Config;
