import { setupWorker } from "msw";
import { apiHandlers } from "./apiHandlers";

export async function initializeMockServiceWorker() {
  const worker = setupWorker(...apiHandlers);
  await worker.start({
    onUnhandledRequest: "bypass",
  });
}
