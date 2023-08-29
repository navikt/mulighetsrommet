import { blockContent } from "./blockContent";
import { tiltakstype } from "./tiltakstype";
import { tiltaksgjennomforing } from "./tiltaksgjennomforing";
import { arrangor } from "./arrangor";
import { navKontaktperson } from "./navKontaktperson";
import { enhet } from "./enhet";
import { regelverklenke } from "./regelverklenke";
import { innsatsgruppe } from "./innsatsgruppe";
import { nokkelinfo } from "./nokkelinfo";
import { faneinnhold } from "./faneinnhold";
import { lenke } from "./lenke";
import { redaktor } from "./redaktor";

export const schemas = [
  // The following are document types which will appear
  // in the studio.
  tiltakstype,
  tiltaksgjennomforing,
  arrangor,
  navKontaktperson,
  enhet,
  regelverklenke,
  innsatsgruppe,
  nokkelinfo,
  faneinnhold,
  lenke,
  forskningsrapport,
  redaktor,
  // When added to this list, object types can be used as
  // { type: 'typename' } in other document schemas
  blockContent,
];
