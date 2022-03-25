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
