import './polyfill';
import Navspa from '@navikt/navspa';
import { createRoot } from 'react-dom/client';
import App, { AppProps } from './App';
import '@navikt/ds-css'; // Importer global css etter app så blir det seende likt ut lokalt og i dev/prod-miljø
import { APPLICATION_NAME } from './constants';

Navspa.eksporter(APPLICATION_NAME, App);

if (import.meta.env.VITE_MULIGHETSROMMET_API_MOCK === 'true') {
  import('./mock/worker')
    .then(({ initializeMockServiceWorker }) => {
      return initializeMockServiceWorker();
    })
    .then(render)
    .catch(error => {
      // eslint-disable-next-line no-console
      console.error('Error occurred while initializing MSW', error);
    });
} else {
  render();
}

/**
 * Applikasjonen har blitt eksponert via NAVSPA og blir inkludert i `veilarbpersonflate`.
 *
 * Vi sjekker eksplisitt om finnes en node med navnet `APPLICATION_NAME` før vi rendrer applikasjonen, da denne også er
 * definert i `index.html` (men ikke i `veilarbpersonflate`).
 */
function render() {
  const container = document.getElementById(APPLICATION_NAME);
  if (container) {
    const root = createRoot(container);
    const MulighetsrommetVeilederFlate = Navspa.importer<AppProps>(APPLICATION_NAME);

    root.render(<MulighetsrommetVeilederFlate fnr={'12345678910'} enhet={'0106'} />);
  }
}
