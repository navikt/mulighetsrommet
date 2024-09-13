import { setupServer } from "msw/node";
import { handlers } from "./handlers";

export function initializeMockServer() {
  setupServer(...handlers).listen();
}
