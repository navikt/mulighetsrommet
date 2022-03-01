import React from 'react';
import ReactDOM from 'react-dom';
import App from './App';
import '@navikt/ds-css';
import './index.less';
import { OpenAPI } from './api';
import Navspa from '@navikt/navspa';
import { worker } from './mock/worker';

OpenAPI.BASE = String(import.meta.env.VITE_BACKEND_API_ROOT);

worker.start();
ReactDOM.render(<App />, document.getElementById('mulighetsrommet-root'));

//TODO skal se på dette sammen med Håkon
// if (import.meta.env.VITE_ENABLE_MOCK === true) {
//   console.log('mock true?', import.meta.env.VITE_ENABLE_MOCK);
//   worker.start();
// ReactDOM.render(<App />, document.getElementById('mulighetsrommet-root'));
// } else {
//   console.log('mock false?', import.meta.env.VITE_ENABLE_MOCK);
//   Navspa.eksporter('mulighetsrommet-flate', App);
// }
