import S from "@sanity/desk-tool/structure-builder";
import { GrDocumentPerformance, GrLocation } from "react-icons/gr";
import { FaWpforms } from "react-icons/fa";
import { GrUserAdmin } from "react-icons/gr";
import { ImOffice } from "react-icons/im";

const ORDER_BY_CREATEDAT_FIELD = [{ field: "_createdAt", direction: "desc" }];

export function commonStructure(S, Context) {
  return [
    S.listItem()
      .title("Tiltaksgjennomføringer")
      .icon(GrDocumentPerformance)
      .child(
        S.list()
          .title("Filter")
          .items([
            S.listItem()
              .title("Alle tiltaksgjennomføringer")
              .icon(GrDocumentPerformance)
              .child(
                S.documentList()
                  .title("Alle tiltaksgjennomføringer")
                  .filter('_type == "tiltaksgjennomforing"')
                  .defaultOrdering([{ field: "_createdAt", direction: "desc" }])
              ),
            S.divider(),
            S.listItem()
              .title("Per enhet")
              .icon(ImOffice)
              .child(
                S.documentTypeList("enhet")
                  .title("Per enhet")
                  .filter('type == "Lokal"')
                  .defaultOrdering([
                    {
                      field: "navn",
                      direction: "asc",
                      ...ORDER_BY_CREATEDAT_FIELD,
                    },
                  ])
                  .child((enhet) =>
                    S.documentList()
                      .defaultOrdering(ORDER_BY_CREATEDAT_FIELD)
                      .title("Tiltaksgjennomføringer")
                      .filter(
                        '_type == "tiltaksgjennomforing" && $enhet in enheter[]._ref'
                      )
                      .params({ enhet })
                      .menuItems([
                        ...S.documentTypeList(
                          "tiltaksgjennomforing"
                        ).getMenuItems(),
                      ])
                  )
              ),
            S.listItem()
              .title("Per fylke")
              .icon(GrLocation)
              .child(
                S.documentTypeList("enhet")
                  .title("Per fylke")
                  .filter('type == "Fylke"')
                  .defaultOrdering([
                    {
                      field: "navn",
                      direction: "asc",
                    },
                  ])
                  .child((enhet) =>
                    S.documentList()
                      .defaultOrdering(ORDER_BY_CREATEDAT_FIELD)
                      .title("Tiltaksgjennomføringer")
                      .filter(
                        '_type == "tiltaksgjennomforing" && ($enhet == fylke._ref)'
                      )
                      .params({ enhet })
                      .menuItems([
                        ...S.documentTypeList(
                          "tiltaksgjennomforing"
                        ).getMenuItems(),
                      ])
                  )
              ),

            S.listItem()
              .title("Per redaktør")
              .icon(GrUserAdmin)
              .child(
                S.documentTypeList("redaktor")
                  .title("Per redaktør")
                  .child((redaktorId) =>
                    S.documentList()
                      .title("Per redaktør")
                      .filter(
                        '_type == "tiltaksgjennomforing" && $redaktorId in redaktor[]._ref'
                      )
                      .params({ redaktorId })
                      .defaultOrdering(ORDER_BY_CREATEDAT_FIELD)
                      .menuItems([
                        ...S.documentTypeList(
                          "tiltaksgjennomforing"
                        ).getMenuItems(),
                      ])
                  )
              ),
            S.listItem()
              .title("Per tiltakstype")
              .icon(FaWpforms)
              .child(
                S.documentTypeList("tiltakstype")
                  .title("Per tiltakstype")
                  .defaultOrdering([
                    {
                      field: "tiltakstypeNavn",
                      direction: "asc",
                      ...ORDER_BY_CREATEDAT_FIELD,
                    },
                  ])
                  .child((tiltakstype) =>
                    S.documentList()
                      .defaultOrdering(ORDER_BY_CREATEDAT_FIELD)
                      .title("Tiltaksgjennomføringer")
                      .filter(
                        '_type == "tiltaksgjennomforing" && $tiltakstype == tiltakstype._ref'
                      )
                      .params({ tiltakstype })
                  )
              ),
            S.listItem()
              .title("Sluttdato har passert")
              .icon(GrDocumentPerformance)
              .child(
                S.documentList()
                  .title("Sluttdato har passert")
                  .filter(
                    '_type == "tiltaksgjennomforing" && defined(sluttdato) && sluttdato < now()'
                  )
                  .defaultOrdering([{ field: "_createdAt", direction: "desc" }])
              ),
          ])
      ),
  ];
}
