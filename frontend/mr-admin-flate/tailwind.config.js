import navPreset from "@navikt/ds-tailwind";
import path from "path";

/** @type {import('tailwindcss').Config} */
export default {
  content: [
    "./src/**/*.{js,jsx,ts,tsx}",
    path.join(
      path.dirname(require.resolve("@mr/frontend-common/package.json")),
      "**/*.{js,jsx,ts,tsx}",
    ),
  ],
  presets: [navPreset],
};
