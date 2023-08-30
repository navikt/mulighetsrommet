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
                      `*[_type == 'tiltaksgjennomforing' && tiltakstype._ref == '${tiltakstype}']{kontaktinfoTiltaksansvarlige[]->}`,
                    );
                  const kontaktPersonIder = result?.map((r) => {
                    return r.kontaktinfoTiltaksansvarlige?.[0]?._id;
                  });
                  return S.documentList()
                    .title("NAV-kontaktperson")
                    .filter(
                      `_type == 'navKontaktperson' && _id in $kontaktPersonIder`,
                    )
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
    ["regelverkfil", "regelverklenke"].includes(listItem.getId()),
  ),
];

export default redaktorAvdirStructure;
