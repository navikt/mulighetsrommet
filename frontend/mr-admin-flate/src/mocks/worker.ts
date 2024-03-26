import { setupWorker } from "msw/browser";
import { apiHandlers } from "./apiHandlers";

export function initializeMockServiceWorker() {
  const worker = setupWorker(...apiHandlers);
  return worker.start({
    onUnhandledRequest: "bypass",
  });
}
