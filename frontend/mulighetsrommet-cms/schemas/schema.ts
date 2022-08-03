import createSchema from "part:@sanity/base/schema-creator";
import schemaTypes from "all:part:@sanity/base/schema-type";
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
import nokkelinfo from "./nokkelinfo";
import faneinnhold from "./faneinnhold";

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
    regelverksfil,
    regelverklenke,
    innsatsgruppe,
    statistikkfil,
    nokkelinfo,
    faneinnhold,

    // When added to this list, object types can be used as
    // { type: 'typename' } in other document schemas
    blockContent,
  ]),
});
