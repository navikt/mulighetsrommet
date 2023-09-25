import { useSendEventTilApi } from "./queries/useSendEventTilApi";
import { erPreview } from "../../utils/Utils";

export const logEvent = (logTag: string, fields?: {}, tags?: {}): void => {
  const erPreviewModus = erPreview || import.meta.env.VITE_MULIGHETSROMMET_API_MOCK === "true";

  if (!erPreviewModus) {
    useSendEventTilApi({ name: logTag, fields, tags });
  } else {
    // eslint-disable-next-line no-console
    console.log("Event", logTag, "Fields:", fields, "Tags:", tags);
  }
};
