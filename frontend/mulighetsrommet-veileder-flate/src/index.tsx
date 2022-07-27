import { createRoot } from 'react-dom/client';
import React from 'react';
import App from './App';
import '@navikt/ds-css';
import { OpenAPI } from 'mulighetsrommet-api-client';
import Navspa from '@navikt/navspa';
import { worker } from './mock/worker';
import { APPLICATION_NAME } from './constants';
import { headers, toRecord } from './api/headers';
import * as Sentry from '@sentry/react';
import { BrowserTracing } from '@sentry/tracing';

Sentry.init({
  dsn: 'https://f7b32ee7a97d4134b3e1120b75f6eb00@sentry.gc.nav.no/132',
  integrations: [new BrowserTracing()],

  // Set tracesSampleRate to 1.0 to capture 100%
  // of transactions for performance monitoring.
  // We recommend adjusting this value in production
  tracesSampleRate: 0.8, // https://docs.sentry.io/platforms/javascript/performance/#configure-the-sample-rate
});

OpenAPI.HEADERS = toRecord(headers);

OpenAPI.BASE = String(import.meta.env.VITE_MULIGHETSROMMET_API_BASE ?? '');

if (import.meta.env.VITE_MULIGHETSROMMET_API_MOCK === 'true') {
  worker.start();
}

Navspa.eksporter(APPLICATION_NAME, App);

// Only mount the application when the root node is available
const container = document.getElementById(APPLICATION_NAME);
if (container) {
  const root = createRoot(container);
  const MulighetsrommetVeilederFlate = Navspa.importer(APPLICATION_NAME);

  root.render(<MulighetsrommetVeilederFlate />);
}
