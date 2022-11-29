import { APPLICATION_NAME } from '../../constants';

export const headers = new Headers();

headers.append('Nav-Consumer-Id', APPLICATION_NAME);
headers.append('nav-norskident', getNorskidentFraUrl());

if (import.meta.env.VITE_MULIGHETSROMMET_API_AUTH_TOKEN) {
  headers.append('Authorization', `Bearer ${import.meta.env.VITE_MULIGHETSROMMET_API_AUTH_TOKEN}`);
}

export function toRecord(headers: Headers) {
  const record: Record<string, string> = {};

  headers.forEach((value, key) => {
    record[key] = value;
  });

  return record;
}

/**
 * Hent bruker i kontekst sin norskIdent (fnr/d-nummer) slik at vi kan gjøre tilgangssjekk i backend
 * @returns norskident for bruker fra url
 */
function getNorskidentFraUrl() {
  const fnrRegex = /\/(\d*)/g;
  const regexMatches = fnrRegex.exec(window.location.pathname);
  return regexMatches?.at(1) ?? '';
}
