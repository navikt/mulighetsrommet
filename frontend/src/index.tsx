import React from 'react';
import ReactDOM from 'react-dom';
import App from './App';
import '@navikt/ds-css';
import './index.less';
import './views/ViewTiltakstype-tiltaksgjennomforing-detaljer.less';
import { OpenAPI } from './api';
import Navspa from '@navikt/navspa';
import { worker } from './mock/worker';

OpenAPI.BASE = String(import.meta.env.VITE_BACKEND_API_ROOT);

if (import.meta.env.VITE_ENABLE_MOCK === 'true') {
  worker.start();
  ReactDOM.render(<App />, document.getElementById('mulighetsrommet-root'));
}
// else {
// TODO skal se på dette sammen med Håkon
// Navspa.eksporter('mulighetsrommet-flate', App);
// }
