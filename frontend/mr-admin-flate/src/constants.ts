import { isAnsattDomene, isProduction } from "./environment";

export const APPLICATION_NAME = "mr-admin-flate";

export const PAGE_SIZE = 50;
export const AVTALE_PAGE_SIZE = 50;
export const ARRANGORER_PAGE_SIZE = 50;

export const OPPMOTE_STED_MAX_LENGTH = 500;

export const MIN_START_DATO_FOR_AVTALER = new Date(2020, 0, 1);
export const MAKS_AAR_FOR_AVTALER = 6;

export function previewArbeidsmarkedstiltakUrl(): string {
  const ansattDomene = isAnsattDomene();

  return isProduction
    ? `https://nav-arbeidsmarkedstiltak.${ansattDomene ? "ansatt" : "intern"}.nav.no/preview`
    : `https://nav-arbeidsmarkedstiltak.${ansattDomene ? "ansatt" : "intern"}.dev.nav.no/preview`;
}

export const ENDRINGSMELDINGER_URL = isProduction
  ? "https://arbeidsmarkedstiltak.intern.nav.no"
  : "https://arbeidsmarkedstiltak.intern.dev.nav.no";

export function sanityStudioUrl(): string {
  const ansattDomene = isAnsattDomene();

  return isProduction
    ? `https://mulighetsrommet-sanity-studio.${ansattDomene ? "ansatt" : "intern"}.nav.no/prod`
    : `https://mulighetsrommet-sanity-studio.${ansattDomene ? "ansatt" : "intern"}.nav.no/test`;
}

export const PORTEN_URL = "https://jira.adeo.no/plugins/servlet/desk/portal/541/create/4665";

/**
 * Peker bruker til innlogging via Microsoft/Entra SSO med "select_account" prompt.
 * Dette lar oss bytte mellom kontoer, evt. gjøre en reautentisering av allerede innlogget bruker.
 */
export function selectAccountUrl(): string {
  const ansattDomene = isAnsattDomene();

  return isProduction
    ? `https://tiltaksadministrasjon.${ansattDomene ? "ansatt" : "intern"}.nav.no/oauth2/login?prompt=select_account`
    : `https://tiltaksadministrasjon.${ansattDomene ? "ansatt" : "intern"}.dev.nav.no/oauth2/login?prompt=select_account`;
}

export const TILGANGER_DOKUMENTASJON_URL =
  "https://navno.sharepoint.com/:fl:/g/contentstorage/CSP_11960ac4-f590-409d-8872-73a98cf165b4/EawaFw1m8WNOqXUHldvs2isBUAaAZzfSGWqwJ3UKLKcvzw?e=MMEdPv&nav=cz0lMkZjb250ZW50c3RvcmFnZSUyRkNTUF8xMTk2MGFjNC1mNTkwLTQwOWQtODg3Mi03M2E5OGNmMTY1YjQmZD1iJTIxeEFxV0VaRDFuVUNJY25PcGpQRmx0RkhKaW55M1VyNUtybS0wWWZEVlN0d3J4WHhVczNFMlNLMHRRRVYxTTZ4SSZmPTAxN0RCNEwyRk1ESUxRMlpYUk1OSEtTNUlIU1hONlpXUkwmYz0lMkYmYT1Mb29wQXBwJnA9JTQwZmx1aWR4JTJGbG9vcC1wYWdlLWNvbnRhaW5lciZ4PSU3QiUyMnclMjIlM0ElMjJUMFJUVUh4dVlYWnVieTV6YUdGeVpYQnZhVzUwTG1OdmJYeGlJWGhCY1ZkRldrUXhibFZEU1dOdVQzQnFVRVpzZEVaSVNtbHVlVE5WY2pWTGNtMHRNRmxtUkZaVGRIZHllRmg0VlhNelJUSlRTekIwVVVWV01VMDJlRWw4TURFM1JFSTBUREpDU3pkVU0xRklSa3hGVGtKSVRFNUpOMHMzVFZKYVZrWldTdyUzRCUzRCUyMiUyQyUyMmklMjIlM0ElMjIxM2Y1MGZlYi0wODAwLTRhMTQtOTkxZS05YWM3MDY0NWFjZmUlMjIlN0Q%3D";
