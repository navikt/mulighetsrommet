import type { Config } from "tailwindcss";
import path from "path";

export default {
  content: [
    "./app/**/{**,.client,.server}/**/*.{js,jsx,ts,tsx}",
    path.join(
      path.dirname(require.resolve("@mr/frontend-common/package.json")),
      "**/*.{js,jsx,ts,tsx}",
    ),
  ],
  theme: {
    extend: {},
  },
  plugins: [],
  presets: [require("@navikt/ds-tailwind")],
} satisfies Config;
