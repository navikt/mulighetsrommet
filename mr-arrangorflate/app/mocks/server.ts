import { setupServer } from "msw/node";
import { handlers } from "./handlers";

export const configureMockServer = () => setupServer(...handlers);
