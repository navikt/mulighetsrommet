import { commonStructure } from "./commonStructure";
import { FaWpforms } from "react-icons/fa";
import { GrUserWorker } from "react-icons/gr";
import { API_VERSION } from "../sanity.config";

const redaktorAvdirStructure = (S, context) => [
  ...commonStructure(S, context),
  S.divider(),
  S.listItem()
    .title("Tiltakstyper")
    .icon(FaWpforms)
    .child(
      S.list()
        .title("Filter")
        .items([
          S.listItem()
            .title("Alle tiltakstyper")
            .icon(FaWpforms)
            .child(S.documentTypeList("tiltakstype").title("Velg tiltakstype")),
          S.divider(),
          S.listItem()
            .title("Kontaktperson per tiltakstype")
            .icon(GrUserWorker)
            .child(
              S.documentTypeList("tiltakstype")
                .title("Velg tiltakstype")
                .child(async (tiltakstype) => {
                  const result = await context
                    .getClient({ apiVersion: API_VERSION })
                    .fetch(
                      `*[_type == 'tiltaksgjennomforing' && tiltakstype._ref == '${tiltakstype}']{kontaktpersoner[]{navKontaktperson->{_id}}}`,
                    );
                  const kontaktPersonIder = result
                    ?.flatMap((r) => {
                      return r.kontaktpersoner;
                    })
                    .filter((r) => r?.navKontaktperson)
                    .map((r) => {
                      return r.navKontaktperson?._id;
                    });
                  return S.documentList()
                    .apiVersion(API_VERSION)
                    .title("Nav-kontaktperson")
                    .filter(`_type == 'navKontaktperson' && _id in $kontaktPersonIder`)
                    .defaultOrdering([
                      {
                        field: "navn",
                        direction: "asc",
                      },
                    ])
                    .params({ kontaktPersonIder });
                }),
            ),
        ]),
    ),
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
