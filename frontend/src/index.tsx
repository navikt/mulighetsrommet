import React from 'react';
import ReactDOM from 'react-dom';
import App from './App';
import '@navikt/ds-css';
import './index.less';
import store from './core/redux/Store';
import { Provider } from 'react-redux';
import { OpenAPI } from './api';
import Navspa from '@navikt/navspa';

OpenAPI.BASE = String(process.env.REACT_APP_BACKEND_API_ROOT);

require('dotenv').config();

if (process.env.REACT_APP_ENABLE_MOCK) {
  const { worker } = require('./mock/worker');
  worker.start();
}

if (process.env.REACT_APP_ENABLE_MOCK) {
  ReactDOM.render(
    <Provider store={store}>
      <App />
    </Provider>,
    document.getElementById('mulighetsrommet-root')
  );
} else {
  Navspa.eksporter('mulighetsrommet-flate', App);
}
