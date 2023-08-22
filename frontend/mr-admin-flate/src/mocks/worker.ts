import { setupWorker } from "msw";
import { apiHandlers } from "./apiHandlers";

export function initializeMockServiceWorker() {
  const worker = setupWorker(...apiHandlers);
  worker.start({
    onUnhandledRequest: "bypass",
  });
}
