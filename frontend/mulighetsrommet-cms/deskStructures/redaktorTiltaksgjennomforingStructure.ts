import { GrDocumentPerformance } from "react-icons/gr";
import { commonStructure } from "./commonStructure";
import { API_VERSION } from "../sanity.config";

const redaktorTiltaksgjennomforingStructure = (S, context) => [
  S.listItem()
    .title("Mine tiltaksgjennomføringer")
    .icon(GrDocumentPerformance)
    .child(
      S.documentList()
        .apiVersion(API_VERSION)
        .title("Mine tiltaksgjennomføringer")
        .filter('_type == "tiltaksgjennomforing" && $redaktorNavn in redaktor[]->navn')
        .params({ redaktorNavn: context.currentUser.name })
        .defaultOrdering([{ field: "_createdAt", direction: "desc" }]),
    ),
  ...commonStructure(S, context),
  S.divider(),
  ...S.documentTypeListItems().filter((listItem) =>
    ["navKontaktperson", "redaktor", "oppskrift", "arrangor", "arrangorKontaktperson"].includes(listItem.getId()),
  ),
];

export default redaktorTiltaksgjennomforingStructure;
