import { commonStructure } from "./commonStructure";

const adminStructure = (S, context) => [
  ...commonStructure(S, context),
  S.divider(),
  ...S.documentTypeListItems().filter(
    (listItem) => !["tiltaksgjennomforing"].includes(listItem.getId())
  ),
];

export default adminStructure;
