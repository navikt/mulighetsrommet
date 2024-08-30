import { setupWorker } from "msw/browser";
import { handlers } from "./handlers";

export const configureMockWorker = () => setupWorker(...handlers);
