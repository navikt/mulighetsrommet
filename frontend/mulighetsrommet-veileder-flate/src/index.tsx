import '@navikt/ds-css';
import Navspa from '@navikt/navspa';
import { OpenAPI } from 'mulighetsrommet-api-client';
import { createRoot } from 'react-dom/client';
import App from './App';
import { APPLICATION_NAME } from './constants';
import { initializeMockWorker } from './mock/worker';

initializeMockWorker();

Navspa.eksporter(APPLICATION_NAME, App);

// Only mount the application when the root node is available
const container = document.getElementById(APPLICATION_NAME);
if (container) {
  const root = createRoot(container);
  const MulighetsrommetVeilederFlate = Navspa.importer(APPLICATION_NAME);

  root.render(<MulighetsrommetVeilederFlate />);
}
