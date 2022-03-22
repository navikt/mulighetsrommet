import React from 'react';
import ReactDOM from 'react-dom';
import App from './App';
import '@navikt/ds-css';
import './index.less';
import { OpenAPI } from 'mulighetsrommet-api';
import Navspa from '@navikt/navspa';
import { worker } from './mock/worker';

console.log('mvf', import.meta.env);

OpenAPI.BASE = String(import.meta.env.VITE_BACKEND_API_ROOT ?? '');

if (import.meta.env.VITE_ENABLE_MOCK === 'true') {
  worker.start();
  ReactDOM.render(<App />, document.getElementById('mulighetsrommet-root'));
} else {
  console.log('mvf', 'export');
  Navspa.eksporter('mulighetsrommet-veileder-flate', App);
}
