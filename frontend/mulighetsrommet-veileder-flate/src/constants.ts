export const APPLICATION_NAME = "mulighetsrommet-veileder-flate";
export const APPLICATION_WEB_COMPONENT_NAME = "mulighetsrommet-arbeidsmarkedstiltak";
export const PORTEN_URL = "https://jira.adeo.no/plugins/servlet/desk/portal/541/create/1401";

export const PORTEN_URL_FOR_TILBAKEMELDING = (tiltaksnummer: string = "", fylke: string = "") =>
  `https://jira.adeo.no/plugins/servlet/desk/portal/541/create/5506?summary="test"&${customField_fylke}=${fylke}&${customField_tiltaksnummer}=${tiltaksnummer}`;

const customField_tiltaksnummer = "customfield_30210";
const customField_fylke = "customfield_33211";
