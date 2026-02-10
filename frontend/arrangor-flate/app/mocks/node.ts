import { setupServer } from "msw/node";
import { handlers } from "./handlers";

let initialized = false;

export function initializeMockServer() {
  if (initialized) return;
  initialized = true;
  setupServer(...handlers).listen();
}
