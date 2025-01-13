import { SetupWorker, setupWorker } from "msw/browser";
import { apiHandlers } from "./apiHandlers";

export function initializeMockServiceWorker(): SetupWorker {
  const worker = setupWorker(...apiHandlers);
  return worker;
}
