import ReactDOM from 'react-dom';
import React from 'react';
import App from './App';
import '@navikt/ds-css';
import './index.less';
import { OpenAPI } from 'mulighetsrommet-api';
import Navspa from '@navikt/navspa';
import { worker } from './mock/worker';
import { APPLICATION_NAME } from './constants';

OpenAPI.BASE = String(import.meta.env.VITE_BACKEND_API_ROOT ?? '');

if (import.meta.env.VITE_ENABLE_MOCK === 'true') {
  worker.start();
}

Navspa.eksporter(APPLICATION_NAME, App);

// Only mount the application when the root node is available
const root = document.getElementById(APPLICATION_NAME);
if (root) {
  const MulighetsrommetVeilederFlate = Navspa.importer(APPLICATION_NAME);
  ReactDOM.render(React.createElement(MulighetsrommetVeilederFlate), root);
}
