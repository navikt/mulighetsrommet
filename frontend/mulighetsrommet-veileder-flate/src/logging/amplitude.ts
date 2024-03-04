import * as amplitude from "@amplitude/analytics-browser";
import { useAtomValue } from "jotai";
import { Event } from "./taxonomy";
import { modiaContextAtom } from "../apps/modia/hooks/useModiaContext";

type LogEvent = (params: { eventName: string; eventData?: any }) => void;

// Default, altså for LOKAL og hvis amplitude skulle være nede, så gjør vi ingen ting
let amplitudeLogger: LogEvent = () => {};

export function initAmplitudeModia() {
  if (window.veilarbpersonflatefsAmplitude) {
    amplitudeLogger = (params: { eventName: string; eventData?: any }) => {
      window.veilarbpersonflatefsAmplitude({
        origin: "arbeidsmarkedstiltak",
        eventName: params.eventName,
        eventData: params.eventData,
      });
    };
  } else {
    // eslint-disable-next-line no-console
    console.warn("Amplitude finnes ikke på window fra veilarbpersonflate");
  }
}

export function initAmplitudeNav() {
  amplitude.init(import.meta.env.VITE_AMPLITUDE_API_KEY, {
    serverUrl: "https://amplitude.nav.no/collect",
    serverZone: "EU",
    instanceName: "nav-arbeidsmarkedstiltak",
    defaultTracking: true,
  });
  amplitudeLogger = (params: { eventName: string; eventData?: any }) =>
    amplitude.logEvent(params.eventName, params.eventData);
}

export function useLogEvent() {
  const contextData = useAtomValue(modiaContextAtom);

  function logEvent(event: Event, extraData?: Record<string, unknown>) {
    const fylkeOgEnhet = contextData
      ? { fylke: contextData.overordnetEnhet, enhet: contextData.enhet }
      : undefined;

    if (import.meta.env.VITE_MULIGHETSROMMET_API_MOCK === "true") {
      // eslint-disable-next-line no-console
      console.log("[Mock Amplitude Event]", {
        name: event.name,
        data: {
          ...("data" in event ? event.data : {}),
          ...extraData,
        },
      });
    } else {
      amplitudeLogger({
        eventName: event.name,
        eventData: {
          ...("data" in event ? event.data : {}),
          ...extraData,
          ...fylkeOgEnhet,
        },
      });
    }
  }

  return { logEvent };
}
