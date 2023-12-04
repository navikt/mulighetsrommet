import * as amplitude from "@amplitude/analytics-browser";
import { AmplitudeEvent } from "./taxonomy";
import { erPreview } from "../utils/Utils";

export function initAmplitude() {
  if (import.meta.env.VITE_MULIGHETSROMMET_API_MOCK === "true") {
    // eslint-disable-next-line no-console
    console.info("Initialiserer ikke Amplitude lokalt når vi kjører mocks");
    return;
  }

  const apiKey = import.meta.env.VITE_AMPLITUDE_KEY ?? "default";

  amplitude.init(apiKey, undefined, {
    serverUrl: import.meta.env.VITE_AMPLITUDE_API_URL,
    defaultTracking: {
      pageViews: {
        trackHistoryChanges: "pathOnly",
        trackOn: () => !erPreview(),
      },
    },
    ingestionMetadata: {
      sourceName: window.location.toString(),
    },
  });
}

export async function logAmplitudeEvent(
  event: AmplitudeEvent,
  extraData?: Record<string, unknown>,
): Promise<void> {
  try {
    amplitude.track(event.name, { ...("data" in event ? event.data : {}), ...extraData });
  } catch (e) {
    // eslint-disable-next-line no-console
    console.error(e);
  }
}
