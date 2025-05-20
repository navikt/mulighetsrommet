import { isProduction } from "@/environment";

export const APPLICATION_NAME = "mulighetsrommet-veileder-flate";
export const APPLICATION_WEB_COMPONENT_NAME = "mulighetsrommet-arbeidsmarkedstiltak";
export const PORTEN_URL = "https://jira.adeo.no/plugins/servlet/desk/portal/541/create/1401";

export const PORTEN_URL_FOR_TILBAKEMELDING = (tiltaksnummer: string = "", fylke: string = "") =>
  `${PORTEN_URL_CONFIG.prod.baseUrl}?${PORTEN_URL_CONFIG.prod.customField_fylke}=${fylke}&${PORTEN_URL_CONFIG.prod.customField_tiltaksnummer}=${encodeURIComponent(tiltaksnummer)}`;

const PORTEN_URL_CONFIG = {
  prod: {
    customField_tiltaksnummer: "customfield_30210",
    customField_fylke: "customfield_34510",
    baseUrl: "https://jira.adeo.no/plugins/servlet/desk/portal/741/create/5593",
  },
  dev: {
    customField_tiltaksnummer: "customfield_30210",
    customField_fylke: "customfield_34510",
    baseUrl: "https://jira-q1.adeo.no/plugins/servlet/desk/portal/761/create/5606",
  },
};

export const TEAM_TILTAK_TILTAKSGJENNOMFORING_APP_URL = isProduction
  ? "https://tiltaksgjennomforing.intern.nav.no/tiltaksgjennomforing"
  : "https://tiltaksgjennomforing.intern.dev.nav.no/tiltaksgjennomforing";
