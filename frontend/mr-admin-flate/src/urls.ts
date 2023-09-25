import { getEnvironment } from "./api/getEnvironment";

export const DELTAKERLISTE_KOMET = {
  local: "http://localhost:4173",
  development: "https://www.intern.dev.nav.no/amt/amt-deltakerliste-flate", // URL til bundle som blir hostet et sted i dev
  production: "", // URL til bundle som blir hostet et sted i prod
};

export const deltakerlisteKometCdnUrl = `${DELTAKERLISTE_KOMET[getEnvironment()]}/bundle.js`;
export const deltakerlisteKometManifestUrl = `${
  DELTAKERLISTE_KOMET[getEnvironment()]
}/manifest.json`;
