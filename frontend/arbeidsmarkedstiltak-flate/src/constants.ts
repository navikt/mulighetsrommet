import { isProduction } from "@/environment";

export const APPLICATION_NAME = "arbeidsmarkedstiltak-flate";
export const APPLICATION_WEB_COMPONENT_NAME = "mulighetsrommet-arbeidsmarkedstiltak";
export const PORTEN_URL = "https://jira.adeo.no/plugins/servlet/desk/portal/541/create/1401";

export const TEAM_TILTAK_TILTAKSGJENNOMFORING_APP_URL = isProduction
  ? "https://tiltaksgjennomforing.intern.nav.no/tiltaksgjennomforing"
  : "https://tiltaksgjennomforing.intern.dev.nav.no/tiltaksgjennomforing";
