import * as amplitude from "@amplitude/analytics-browser";
import { getEnvironment } from "./api/getEnvironment";

function getApiKeyFromEnvironment() {
  switch (getEnvironment()) {
    case "production":
      return ""; // TODO Legg inn ApiKey for Amplitude i prod
    case "development":
      return ""; // TODO Legg inn ApiKey for Amplitude i dev
    case "local":
      return ""; // TODO Legg inn ApiKey for Amplitude lokalt?
  }
}

function initializeAmplitude() {
  amplitude.init(getApiKeyFromEnvironment(), {
    serverUrl: "amplitude.nav.no/collect",
    serverZone: "EU",
    instanceName: "nav-tiltaksadministrasjon",
  });
}

export { amplitude, initializeAmplitude };
