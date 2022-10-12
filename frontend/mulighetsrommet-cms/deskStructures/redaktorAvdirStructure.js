import S from "@sanity/desk-tool/structure-builder";
import { commonStructure } from "./commonStructure";

const redaktorAvdirStructure = [
  ...commonStructure(),
  S.divider(),
  ...S.documentTypeListItems().filter(
    (listItem) =>
      ![
        "tiltaksgjennomforing",
        "enhet",
        "navKontaktperson",
        "arrangor",
        "regelverkfil",
        "regelverklenke",
        "forskningsrapport",
        "innsatsgruppe",
        "statistikkfil",
        "redaktor",
      ].includes(listItem.getId())
  ),
  S.divider(),
  ...S.documentTypeListItems().filter((listItem) =>
    ["navKontaktperson", "arrangor"].includes(listItem.getId())
  ),
  S.divider(),
  ...S.documentTypeListItems().filter((listItem) =>
    ["regelverkfil", "regelverklenke", "forskningsrapport"].includes(
      listItem.getId()
    )
  ),
];

export default redaktorAvdirStructure;
