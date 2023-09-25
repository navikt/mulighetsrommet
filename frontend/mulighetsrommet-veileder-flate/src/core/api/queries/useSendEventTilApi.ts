import { FrontendEvent } from "mulighetsrommet-api-client";
import { mulighetsrommetClient } from "../clients";

export function useSendEventTilApi(event: FrontendEvent) {
  mulighetsrommetClient.internal.logFrontendEvent({ requestBody: event });
}
