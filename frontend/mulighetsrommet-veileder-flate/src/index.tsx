import './polyfill';
import Navspa from '@navikt/navspa';
import { createRoot } from 'react-dom/client';
import App, { AppProps } from './App';
import '@navikt/ds-css'; // Importer global css etter app så blir det seende likt ut lokalt og i dev/prod-miljø
import { APPLICATION_NAME } from './constants';
import { initializeMockWorker } from './mock/worker';

initializeMockWorker();

Navspa.eksporter(APPLICATION_NAME, App);

// Only mount the application when the root node is available
const container = document.getElementById(APPLICATION_NAME);
if (container) {
  const root = createRoot(container);
  const MulighetsrommetVeilederFlate = Navspa.importer<AppProps>(APPLICATION_NAME);

  root.render(<MulighetsrommetVeilederFlate fnr={'12345678910'} enhet={'0106'} />);
}
