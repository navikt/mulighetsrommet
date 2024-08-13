export const APPLICATION_NAME = "mulighetsrommet-veileder-flate";
export const APPLICATION_WEB_COMPONENT_NAME = "mulighetsrommet-arbeidsmarkedstiltak";
export const PORTEN_URL = "https://jira.adeo.no/plugins/servlet/desk/portal/541/create/1401";

export const PORTEN_URL_FOR_TILBAKEMELDING = (tiltaksnummer: string = "", fylke: string = "") =>
  `${PORTEN_URL_CONFIG.dev.baseUrl}?${PORTEN_URL_CONFIG.dev.customField_fylke}=${fylke}&${PORTEN_URL_CONFIG.dev.customField_tiltaksnummer}=${tiltaksnummer}`;

// TODO Bytt til korrekt prod-url når skjema er opprettet av Ingunn i prod
const PORTEN_URL_CONFIG = {
  prod: {
    customField_tiltaksnummer: "customfield_30210",
    customField_fylke: "customfield_33211",
    baseUrl: "FIXME",
  },
  dev: {
    customField_tiltaksnummer: "customfield_30210",
    customField_fylke: "customfield_33112",
    baseUrl: "https://jira-q1.adeo.no/plugins/servlet/desk/portal/761/create/5606",
  },
};
