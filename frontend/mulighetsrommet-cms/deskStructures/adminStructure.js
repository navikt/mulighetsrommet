import S from "@sanity/desk-tool/structure-builder";

const adminStructure = [
  S.listItem()
    .title("Filtrerte tiltaksgjennomføringer")
    .child(
      S.list()
        .title("Filter")
        .items([
          S.listItem()
            .title("Per fylke")
            .child(
              S.documentTypeList("enhet")
                .title("Per fylke")
                .filter('type == "Fylke"')
                .defaultOrdering([{ field: "navn", direction: "asc" }])
                .child((enhet) =>
                  S.documentList()
                    .defaultOrdering([
                      { field: "_createdAt", direction: "desc" },
                    ])
                    .title("Tiltaksgjennomføringer")
                    .filter(
                      '_type == "tiltaksgjennomforing" && ($enhet == fylke._ref)'
                    )
                    .params({ enhet })
                )
            ),
          S.listItem()
            .title("Per kontor")
            .child(
              S.documentTypeList("enhet")
                .title("Per kontor")
                .filter('type == "Lokal"')
                .defaultOrdering([{ field: "navn", direction: "asc" }])
                .child((enhet) =>
                  S.documentList()
                    .title("Tiltaksgjennomføringer")
                    .filter(
                      '_type == "tiltaksgjennomforing" && $enhet in enheter[]._ref'
                    )
                    .defaultOrdering([
                      { field: "_createdAt", direction: "desc" },
                    ])
                    .params({ enhet })
                )
            ),
          S.listItem()
            .title("Per tiltakstype")
            .child(
              S.documentTypeList("tiltakstype")
                .title("Per tiltakstype")
                .defaultOrdering([
                  { field: "tiltakstypeNavn", direction: "asc" },
                ])
                .child((tiltakstype) =>
                  S.documentList()
                    .defaultOrdering([
                      { field: "_createdAt", direction: "desc" },
                    ])
                    .title("Tiltaksgjennomføringer")
                    .filter(
                      '_type == "tiltaksgjennomforing" && $tiltakstype == tiltakstype._ref'
                    )
                    .params({ tiltakstype })
                )
            ),
        ])
    ),

  S.listItem()
    .title("Alle tiltaksgjennomføringer")
    .child(
      S.documentList()
        .defaultOrdering([{ field: "_createdAt", direction: "desc" }])
        .title("Alle tiltaksgjennomføringer")
        .filter('_type == "tiltaksgjennomforing"')
    ),
  S.divider(),
  ...S.documentTypeListItems().filter(
    (listItem) => !["tiltaksgjennomforing"].includes(listItem.getId())
  ),
];

export default adminStructure;
