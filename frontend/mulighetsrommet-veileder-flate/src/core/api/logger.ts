import { useSendEventTilApi } from "./queries/useSendEventTilApi";
import { erPreview } from "../../utils/Utils";
import { logAmplitudeEvent } from "../../amplitude/amplitude";
import { AmplitudeEvent } from "../../amplitude/taxonomy";

export const logEvent = (event: AmplitudeEvent): void => {
  const erPreviewModus = erPreview() || import.meta.env.VITE_MULIGHETSROMMET_API_MOCK === "true";

  if (!erPreviewModus) {
    useSendEventTilApi({ name: event.name, ...("data" in event ? event.data : {}) });
    logAmplitudeEvent(event);
  } else {
    // eslint-disable-next-line no-console
    console.log({ name: event.name, ...("data" in event ? event.data : {}) });
  }
};
