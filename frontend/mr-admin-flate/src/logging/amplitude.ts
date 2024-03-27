import * as amplitude from "@amplitude/analytics-browser";
import { Event } from "./taxonomy";
import { Environment, environment } from "@/environment";

type LogEvent = (params: { name: string; data?: any }) => void;

// Default, altså for LOKAL og hvis amplitude skulle være nede, så gjør vi ingen ting
let amplitudeLogger: LogEvent | undefined = undefined;

function getApiKeyFromEnvironment() {
  switch (environment) {
    case Environment.PROD:
      return "94b44c9f436db8d9d42b493fdd7d98c9";
    case Environment.DEV:
      return "42076036673d4b35d862b4282840f30b";
    case Environment.DEMO:
    case Environment.LOCAL:
      return "mock";
  }
}

function initializeAmplitude() {
  const apiKey = getApiKeyFromEnvironment();
  if (apiKey === "mock") {
    amplitudeLogger = (params: { name: string; data?: any }) => {
      // eslint-disable-next-line no-console
      console.log("[Mock Amplitude Event]", {
        name: params.name,
        data: {
          ...("data" in params.data ? params.data.data : {}),
          ...params.data,
        },
      });
    };
  } else {
    amplitude.init(apiKey, {
      serverUrl: "https://amplitude.nav.no/collect",
      serverZone: "EU",
      instanceName: "nav-tiltaksadministrasjon",
      defaultTracking: true,
    });
    amplitudeLogger = (params: { name: string; data?: any }) => {
      amplitude.logEvent(params.name, params.data);
    };
  }
}

function logEvent(event: Event, extraData?: Record<string, unknown>) {
  if (!amplitudeLogger) {
    // eslint-disable-next-line no-console
    console.error("Amplitude er ikke initialisert");
  } else {
    amplitudeLogger({
      name: event.name,
      data: {
        ...("data" in event ? event.data : {}),
        ...extraData,
      },
    });
  }
}

export { logEvent, initializeAmplitude };
