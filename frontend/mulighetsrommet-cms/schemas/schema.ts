import createSchema from "part:@sanity/base/schema-creator";
import schemaTypes from "all:part:@sanity/base/schema-type";
import blockContent from "./blockContent";

import tiltakstype from "./tiltakstype";
import tiltaksgjennomforing from "./tiltaksgjennomforing";
import arrangor from "./arrangor";
import kontaktperson from "./kontaktperson";
import enhet from "./enhet";
import regelverklenke from "./regelverklenke";
import innsatsgruppe from "./innsatsgruppe";
import statistikkfil from "./statistikkfil";
import nokkelinfo from "./nokkelinfo";
import faneinnhold from "./faneinnhold";
import forskningsrapport from "./forskningsrapport";
import lenke from "./lenke";
import redaktor from "./redaktor";

export default createSchema({
  name: "default",
  types: schemaTypes.concat([
    // The following are document types which will appear
    // in the studio.
    tiltakstype,
    tiltaksgjennomforing,
    arrangor,
    kontaktperson,
    enhet,
    regelverklenke,
    innsatsgruppe,
    statistikkfil,
    nokkelinfo,
    faneinnhold,
    lenke,
    forskningsrapport,
    redaktor,
    // When added to this list, object types can be used as
    // { type: 'typename' } in other document schemas
    blockContent,
  ]),
});
