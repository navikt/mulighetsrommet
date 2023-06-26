import { getEnvironment } from "./api/getEnvironment";

export const POC_BASE_URL = {
  local: "http://localhost:4173",
  development: "",
  production: "",
};

export const pocBaseCdnUrl = `${POC_BASE_URL[getEnvironment()]}/bundle.js`;
export const pocManifestUrl = `${POC_BASE_URL[getEnvironment()]}/manifest.json`;
