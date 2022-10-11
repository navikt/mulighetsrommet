import S from "@sanity/desk-tool/structure-builder";
import { commonStructure } from "./commonStructure";
import tiltakstype from "../schemas/tiltakstype";

const redaktorTiltaksgjennomforingStructure = (redaktorNavn) => [
  S.listItem()
    .title("Mine tiltaksgjennomføringer")
    .child(
      S.documentList()
        .title("Mine tiltaksgjennomføringer")
        .filter(
          '_type == "tiltaksgjennomforing" && redaktor -> navn == $redaktorNavn'
        )
        .params({ redaktorNavn })
        .defaultOrdering([{ field: "_createdAt", direction: "desc" }])
    ),
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
        "redaktor",
      ].includes(listItem.getId())
  ),
  S.divider(),
  ...S.documentTypeListItems().filter((listItem) =>
    ["navKontaktperson", "arrangor", "redaktor"].includes(listItem.getId())
  ),
];

export default redaktorTiltaksgjennomforingStructure;
