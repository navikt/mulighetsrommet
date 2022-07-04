import S from "@sanity/desk-tool/structure-builder";

export default () =>
  S.list()
    .title("Innhold")
    .items([
      S.listItem()
        .title("Filtrerte tiltaksgjennomføringer")
        .child(
          S.documentTypeList("enhet")
            .title("Tiltaksgjennomføringer per region")
            .child((enhet) => {
              console.log(enhet);
              return S.documentList()
                .title("Tiltaksgjennomføringer")
                .filter(
                  '_type == "tiltaksgjennomforing" && ($enhet == fylke._ref || $enhet in enheter[]._ref)'
                )
                .params({ enhet });
            })
        ),
      ...S.documentTypeListItems(),
    ]);
