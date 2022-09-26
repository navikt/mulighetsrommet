import S from "@sanity/desk-tool/structure-builder";
import { commonStructure } from "./commonStructure";
const redaktorAvdirStructure = [
  ...commonStructure(),
  S.divider(),
  ...S.documentTypeListItems().filter(
    (listItem) =>
      ![
        "tiltaksgjennomforing",
        "arrangor",
        "navKontaktperson",
        "enhet",
        "innsatsgruppe",
        "statistikkfil",
      ].includes(listItem.getId())
  ),
];

export default redaktorAvdirStructure;
