import { setupWorker } from 'msw';
import { apiHandlers } from './api/apiHandlers';
import { veilarbpersonflateHandlers } from "./api/eksterneSystemerHandlers";

// This configures a Service Worker with the given request handlers.

export function initializeMockWorker() {
  const handlers = [
    ...(import.meta.env.VITE_MULIGHETSROMMET_API_MOCK === 'true' ? apiHandlers : []),
    ...(import.meta.env.VITE_EKSTERNE_SYSTEMER_MOCK === 'true' ? veilarbpersonflateHandlers: []),
  ];
  if (handlers.length !== 0) {
    const worker = setupWorker(...handlers);
    worker.start();
  }
}
