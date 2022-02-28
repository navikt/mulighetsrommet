import React from 'react';
import ReactDOM from 'react-dom';
import App from './App';
import '@navikt/ds-css';
import './index.less';
import { OpenAPI } from './api';
import Navspa from '@navikt/navspa';
import { worker } from './mock/worker';

OpenAPI.BASE = String(process.env.REACT_APP_BACKEND_API_ROOT);

require('dotenv').config();

if (process.env.REACT_APP_ENABLE_MOCK) {
  worker.start();
  const elem = document.createElement('div');
  document.body.appendChild(elem);
  ReactDOM.render(<App />, elem);
} else {
  Navspa.eksporter('mulighetsrommet-flate', App);
}
