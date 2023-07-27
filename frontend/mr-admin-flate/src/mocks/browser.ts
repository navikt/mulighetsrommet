// src/mocks/browser.js
import { setupWorker } from "msw";
import { apiHandlers } from "./apiHandlers";

// This configures a Service Worker with the given request handlers.
const handlers = [
  ...(import.meta.env.VITE_MULIGHETSROMMET_API_MOCK === "true"
    ? apiHandlers
    : []),
];

const worker = setupWorker(...handlers);

export { worker };
