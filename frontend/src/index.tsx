import React from 'react';
import ReactDOM from 'react-dom';
import App from './App';
import '@navikt/ds-css';
import './index.less';
import store from './core/redux/Store';
import { Provider } from 'react-redux';

require('dotenv').config();

ReactDOM.render(
  <Provider store={store}>
    <App />
  </Provider>,
  document.getElementById('mulighetsrommet-root')
);
