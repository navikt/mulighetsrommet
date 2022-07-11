import blockContent from "./blockContent";

import tiltakstype from "./tiltakstype";
import tiltaksgjennomforing from "./tiltaksgjennomforing";
import arrangor from "./arrangor";
import kontaktperson from "./kontaktperson";
import enhet from "./enhet";
import regelverksfil from "./regelverkfil";
import regelverklenke from "./regelverklenke";
import innsatsgruppe from "./innsatsgruppe";
import statistikkfil from "./statistikkfil";

export const schemaTypes = [
  // The following are document types which will appear
  // in the studio.
  tiltakstype,
  tiltaksgjennomforing,
  arrangor,
  kontaktperson,
  enhet,
  regelverksfil,
  regelverklenke,
  innsatsgruppe,
  statistikkfil,

  // When added to this list, object types can be used as
  // { type: 'typename' } in other document schemas
  blockContent,
];
