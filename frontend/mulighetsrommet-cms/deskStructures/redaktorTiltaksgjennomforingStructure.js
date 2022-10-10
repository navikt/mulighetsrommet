import S from "@sanity/desk-tool/structure-builder";
import { commonStructure } from "./commonStructure";
import tiltakstype from "../schemas/tiltakstype";

const redaktorTiltaksgjennomforingStructure = [
  ...commonStructure(),
  ...S.documentTypeListItems().filter(
    (listItem) =>
      ![
        "tiltaksgjennomforing",
        "tiltakstype",
        "enhet",
        "navKontaktperson",
        "arrangor",
        "regelverkfil",
        "regelverklenke",
        "forskningsrapport",
        "innsatsgruppe",
        "statistikkfil",
      ].includes(listItem.getId())
  ),
  S.divider(),
  ...S.documentTypeListItems().filter((listItem) =>
    ["navKontaktperson", "arrangor"].includes(listItem.getId())
  ),
  S.divider(),
  ...S.documentTypeListItems().filter((listItem) =>
    ["forskningsrapport"].includes(listItem.getId())
  ),
];

export default redaktorTiltaksgjennomforingStructure;
