import { setupWorker } from "msw/browser";
import { apiHandlers } from "./api/apiHandlers";

export async function initializeMockServiceWorker() {
  const worker = setupWorker(...apiHandlers);
  await worker.start({
    onUnhandledRequest: "bypass",
  });
}
