import S from "@sanity/desk-tool/structure-builder";
import { commonStructure } from "./commonStructure";

const adminStructure = [
  ...commonStructure(),
  S.divider(),
  ...S.documentTypeListItems().filter(
    (listItem) => !["tiltaksgjennomforing"].includes(listItem.getId())
  ),
];

export default adminStructure;
