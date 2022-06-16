import createSchema from "part:@sanity/base/schema-creator";
import schemaTypes from "all:part:@sanity/base/schema-type";
import blockContent from "./blockContent";
import tiltakstype from "./tiltakstype";
import tiltaksgjennomforing from "./tiltaksgjennomforing";
import arrangor from "./arrangor";
import kontaktperson from "./kontaktperson";

export default createSchema({
  name: "default",
  types: schemaTypes.concat([
    // The following are document types which will appear
    // in the studio.
    tiltakstype,
    tiltaksgjennomforing,
    arrangor,
    kontaktperson,

    // When added to this list, object types can be used as
    // { type: 'typename' } in other document schemas
    blockContent,
  ]),
});
