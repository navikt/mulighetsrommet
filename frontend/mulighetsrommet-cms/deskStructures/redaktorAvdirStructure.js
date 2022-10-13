import S from "@sanity/desk-tool/structure-builder";
import { commonStructure } from "./commonStructure";
import { FaWpforms } from "react-icons/fa";
import { GrUserWorker } from "react-icons/gr";
import sanityClient from "part:@sanity/base/client";
const client = sanityClient.withConfig({ apiVersion: "2021-10-21" });

const redaktorAvdirStructure = [
  ...commonStructure(),
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
                  const result = await client.fetch(
                    `*[_type == 'tiltaksgjennomforing' && tiltakstype._ref == '${tiltakstype}']{kontaktinfoTiltaksansvarlige[]->}`
                  );
                  const kontaktPersonIder = result?.map((r) => {
                    return r.kontaktinfoTiltaksansvarlige[0]._id;
                  });
                  return S.documentList()
                    .title("NAV-kontaktperson")
                    .filter(
                      `_type == 'navKontaktperson' && _id in $kontaktPersonIder`
                    )
                    .defaultOrdering([
                      {
                        field: "navn",
                        direction: "asc",
                      },
                    ])
                    .params({ kontaktPersonIder });
                })
            ),
        ])
    ),
  ...S.documentTypeListItems().filter(
    (listItem) =>
      ![
        "tiltakstype",
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
