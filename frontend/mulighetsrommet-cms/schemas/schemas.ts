import { blockContent } from "./blockContent";
import { tiltakstype } from "./tiltakstype";
import { tiltaksgjennomforing } from "./tiltaksgjennomforing";
import { navKontaktperson } from "./navKontaktperson";
import { enhet } from "./enhet";
import { regelverklenke } from "./regelverklenke";
import { faneinnhold } from "./faneinnhold";
import { forskningsrapport } from "./forskningsrapport";
import { lenke } from "./lenke";
import { redaktor } from "./redaktor";
import { oppskrift } from "./oppskrift/oppskrift";
import { steg } from "./oppskrift/steg";
import { oppskriftContent } from "./oppskrift/oppskriftContent";
import { tips } from "./oppskrift/tips";
import { alertMessage } from "./oppskrift/alertMessage";

export const schemas = [
  // The following are document types which will appear
  // in the studio.
  tiltakstype,
  tiltaksgjennomforing,
  navKontaktperson,
  enhet,
  regelverklenke,
  faneinnhold,
  lenke,
  forskningsrapport,
  redaktor,
  oppskrift,
  steg,
  tips,
  alertMessage,
  oppskriftContent,
  // When added to this list, object types can be used as
  // { type: 'typename' } in other document schemas
  blockContent,
];
