import S from "@sanity/desk-tool/structure-builder";

export default () =>
  S.list()
    .title("Innhold")
    .items([
      S.listItem()
        .title("Filtrerte tiltaksgjennomføringer")
        .child(
          S.documentTypeList("enhet")
            .title("Per fylke")
            .filter('type == "Fylke"')
            .child((enhet) =>
              S.documentList()
                .title("Tiltaksgjennomføringer")
                .filter(
                  '_type == "tiltaksgjennomforing" && ($enhet == fylke._ref || $enhet in enheter[]._ref)'
                )
                .params({ enhet })
            )
        ),
      S.listItem()
        .title("Alle tiltaksgjennomføringer")
        .child(
          S.documentList()
            .title("Alle tiltaksgjennomføringer")
            .filter('_type == "tiltaksgjennomforing"')
        ),
      S.divider(),
      ...S.documentTypeListItems().filter(
        (listItem) => !["tiltaksgjennomforing"].includes(listItem.getId())
      ),
    ]);
