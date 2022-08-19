import { FrontendEvent, MulighetsrommetService } from 'mulighetsrommet-api-client';

const credentials = 'same-origin';

const MED_CREDENTIALS: RequestInit = {
  credentials,
  headers: {
    'Nav-Consumer-Id': 'mulighetsrommet',
    'Content-Type': 'application/json',
  },
};
export function useSendEventTilApi(event: FrontendEvent) {
  MulighetsrommetService.logFrontendEvent({ requestBody: event });
}

export function sendEventTilPortefolje(event: FrontendEvent) {
  const url = `veilarbperson/logger/event`;
  const config = { ...MED_CREDENTIALS, method: 'post', body: JSON.stringify(event) };
  return fetch(url, config);
}
