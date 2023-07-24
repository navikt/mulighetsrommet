import { GrDocumentPerformance } from "react-icons/gr";
import { commonStructure } from "./commonStructure";

const redaktorTiltaksgjennomforingStructure = (S, context) => [
  S.listItem()
    .title("Mine tiltaksgjennomføringer")
    .icon(GrDocumentPerformance)
    .child(
      S.documentList()
        .title("Mine tiltaksgjennomføringer")
        .filter(
          '_type == "tiltaksgjennomforing" && $redaktorNavn in redaktor[]->navn',
        )
        .params({ redaktorNavn: context.currentUser.name })
        .defaultOrdering([{ field: "_createdAt", direction: "desc" }]),
    ),
  ...commonStructure(S, context),
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
        // "forskningsrapport",
        "innsatsgruppe",
        "redaktor",
      ].includes(listItem.getId()),
  ),
  S.divider(),
  ...S.documentTypeListItems().filter((listItem) =>
    ["navKontaktperson", "arrangor", "redaktor"].includes(listItem.getId()),
  ),
];

export default redaktorTiltaksgjennomforingStructure;
