import { FrontendEvent } from 'mulighetsrommet-api-client';
import { mulighetsrommetClient } from '../clients';

const MED_CREDENTIALS: RequestInit = {
  credentials: 'same-origin',
  headers: {
    'Nav-Consumer-Id': 'mulighetsrommet',
    'Content-Type': 'application/json',
  },
};

export function useSendEventTilApi(event: FrontendEvent) {
  mulighetsrommetClient.internal.logFrontendEvent({ requestBody: event });
}

export function sendEventTilPortefolje(event: FrontendEvent) {
  const url = `veilarbperson/logger/event`;
  const config = { ...MED_CREDENTIALS, method: 'post', body: JSON.stringify(event) };
  return fetch(url, config);
}
