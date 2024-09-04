import { setupServer } from "msw/node";
import { handlers } from "./handlers";

export function initializeMockServer() {
  const server = setupServer(...handlers);
  return server.listen({
    onUnhandledRequest: "bypass",
  });
}
