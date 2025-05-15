import { isProduction } from "./environment";

export const APPLICATION_NAME = "mr-admin-flate";

export const PAGE_SIZE = 50;
export const AVTALE_PAGE_SIZE = 50;
export const ARRANGORER_PAGE_SIZE = 50;

export const STED_FOR_GJENNOMFORING_MAX_LENGTH = 100;

export const MIN_START_DATO_FOR_AVTALER = new Date(2000, 0, 1);
export const MAKS_AAR_FOR_AVTALER = 35;

export const PREVIEW_ARBEIDSMARKEDSTILTAK_URL = isProduction
  ? "https://nav-arbeidsmarkedstiltak.intern.nav.no/preview"
  : "https://nav-arbeidsmarkedstiltak.intern.dev.nav.no/preview";

export const ENDRINGSMELDINGER_URL = isProduction
  ? "https://arbeidsmarkedstiltak.intern.nav.no"
  : "https://arbeidsmarkedstiltak.intern.dev.nav.no";

export const SANITY_STUDIO_URL = isProduction
  ? "https://mulighetsrommet-sanity-studio.intern.nav.no/prod"
  : "https://mulighetsrommet-sanity-studio.intern.nav.no/test";

export const PORTEN_URL = "https://jira.adeo.no/plugins/servlet/desk/portal/541/create/4665";

/**
 * Peker bruker til innlogging via Microsoft/Entra SSO med "select_account" prompt.
 * Dette lar oss bytte mellom kontoer, evt. gjøre en reautentisering av allerede innlogget bruker.
 */
export const SELECT_ACCOUNT_URL = isProduction
  ? "https://tiltaksadministrasjon.intern.nav.no/oauth2/login?prompt=select_account"
  : "https://tiltaksadministrasjon.intern.dev.nav.no/oauth2/login?prompt=select_account";
