import * as amplitude from "@amplitude/analytics-browser";
import { getEnvironment } from "./api/getEnvironment";

function getApiKeyFromEnvironment() {
  switch (getEnvironment()) {
    case "production":
      return "94b44c9f436db8d9d42b493fdd7d98c9"; // TODO Legg inn ApiKey for Amplitude i prod
    case "development":
      return "42076036673d4b35d862b4282840f30b";
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
