import './polyfill';
import { createRoot } from 'react-dom/client';
import { APPLICATION_NAME, ARBEIDSMARKEDSTILTAK } from './constants';
import { createElement } from 'react';

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
  //TODO verifiser at dette stemmer
  // Denne må lazy importeres fordi den laster inn all css selv inn under sin egen shadow-root
  return import('./WebComponentWrapper')
    .then(({ Arbeidsmarkedstiltak }) => {
      customElements.define(ARBEIDSMARKEDSTILTAK, Arbeidsmarkedstiltak);
    })
    .then(() => {
      const container = document.getElementById(APPLICATION_NAME);
      if (container) {
        const element = createElement(ARBEIDSMARKEDSTILTAK, { 'data-fnr': '12345678910' });
        const root = createRoot(container);
        root.render(element);
      }
    });
}
