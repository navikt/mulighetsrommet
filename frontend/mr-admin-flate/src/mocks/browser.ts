import { setupWorker } from "msw";
import { apiHandlers } from "./apiHandlers";

const worker = setupWorker(...apiHandlers);

export { worker };
