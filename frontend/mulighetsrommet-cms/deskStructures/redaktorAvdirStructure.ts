import { commonStructure } from "./commonStructure";
import { FaWpforms } from "react-icons/fa";

const redaktorAvdirStructure = (S, context) => [
  ...commonStructure(S, context),
  S.divider(),
  S.listItem()
    .title("Tiltakstyper")
    .icon(FaWpforms)
    .child(S.documentTypeList("tiltakstype").title("Velg tiltakstype")),
  S.divider(),
  ...S.documentTypeListItems().filter((listItem) =>
    ["navKontaktperson"].includes(listItem.getId()),
  ),
  S.divider(),
  ...S.documentTypeListItems().filter((listItem) =>
    // ["regelverkfil", "regelverklenke", "forskningsrapport"].includes(
    ["regelverkfil", "regelverklenke", "oppskrift"].includes(listItem.getId()),
  ),
];

export default redaktorAvdirStructure;
