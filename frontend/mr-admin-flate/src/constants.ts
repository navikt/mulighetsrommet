import { isProduction } from "./environment";

export const APPLICATION_NAME = "mr-admin-flate";

export const PAGE_SIZE = 15;
export const AVTALE_PAGE_SIZE = 15;
export const ARRANGORER_PAGE_SIZE = 15;

export const STED_FOR_GJENNOMFORING_MAX_LENGTH = 100;

export const PREVIEW_ARBEIDSMARKEDSTILTAK_URL = isProduction
  ? "https://nav-arbeidsmarkedstiltak.intern.nav.no/preview"
  : "https://nav-arbeidsmarkedstiltak.intern.dev.nav.no/preview";

export const ENDRINGSMELDINGER_URL = isProduction
  ? "https://arbeidsmarkedstiltak.intern.nav.no"
  : "https://arbeidsmarkedstiltak.intern.dev.nav.no";

export const SANITY_STUDIO_URL = isProduction
  ? "https://mulighetsrommet-sanity-studio.intern.nav.no/prod/desk"
  : "https://mulighetsrommet-sanity-studio.intern.nav.no/test/desk";

export const PORTEN_URL = "https://jira.adeo.no/plugins/servlet/desk/portal/541/create/4665";

export const LOGOUT_AND_SELECT_ACCOUNT_URL = isProduction
  ? "https://tiltaksadministrasjon.intern.nav.no/oauth2/login?prompt=select_account" // Ja, det skal være login, og ikke logout
  : "https://tiltaksadministrasjon.intern.dev.nav.no/oauth2/login?prompt=select_account"; // Ja, det skal være login, og ikke logout;
