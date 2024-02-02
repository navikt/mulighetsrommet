import { useAtomValue } from "jotai";
import { LogEventFromApp } from "../env";
import { erPreview } from "../utils/Utils";
import { Event } from "./taxonomy";
import { modiaContextAtom } from "../apps/modia/hooks/useModiaContext";

let amplitude: LogEventFromApp | null = null;

export function initAmplitude() {
  if (import.meta.env.VITE_MULIGHETSROMMET_API_MOCK === "true") {
    // eslint-disable-next-line no-console
    console.info("Initialiserer ikke Amplitude lokalt når vi kjører mocks");
    return;
  }

  if (window.veilarbpersonflatefsAmplitude) {
    amplitude = window.veilarbpersonflatefsAmplitude;
  } else {
    // eslint-disable-next-line no-console
    console.warn("Amplitude finnes ikke på window fra veilarbpersonflate");
  }
}

export function useLogEvent() {
  const contextData = useAtomValue(modiaContextAtom);

  function logEvent(event: Event, extraData?: Record<string, unknown>) {
    const erPreviewModus = erPreview() || import.meta.env.VITE_MULIGHETSROMMET_API_MOCK === "true";
    const fylkeOgEnhet = { fylke: contextData.overordnetEnhet, enhet: contextData.enhet };
    if (!erPreviewModus) {
      amplitude?.({
        origin: "arbeidsmarkedstiltak",
        eventName: event.name,
        eventData: {
          ...("data" in event ? event.data : {}),
          ...extraData,
          ...fylkeOgEnhet,
        },
      });
    } else {
      // eslint-disable-next-line no-console
      console.log("[Mock Amplitude Event]", {
        name: event.name,
        ...("data" in event ? event.data : {}),
        ...fylkeOgEnhet,
      });
    }
  }

  return { logEvent };
}
