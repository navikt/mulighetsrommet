import S from "@sanity/desk-tool/structure-builder";
import sanityClient from "part:@sanity/base/client";
import React, { useEffect, useState } from "react";

const client = sanityClient.withConfig({ apiVersion: "2021-10-21" });

export default () =>
  S.list()
    .title("Innhold")
    .items([
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
                        .params({ enhet })
                    )
                ),
            ])
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

export const getDefaultDocumentNode = () => {
  return S.document().views([
    S.view.form(),
    S.view.component(JsonPreview).title("Gjennomføring med tiltakstype"),
  ]);
};

function JsonPreview({ document }) {
  const [tiltaksdata, setTiltaksdata] = useState(null);

  useEffect(() => {
    const fetchData = async () => {
      const data = await client.fetch(
        `*[_type == "tiltakstype" && _id == "${document.displayed.tiltakstype._ref}"]{..., innsatsgruppe->}[0]`
      );
      setTiltaksdata(data);
    };

    fetchData();
  }, [document]);

  return (
    <div style={{ padding: "10px" }}>
      <h1>Visning av tiltaksgjennomføring med tiltakstype</h1>
      <div style={{ display: "grid", gridTemplateColumns: "repeat(2, 1fr)" }}>
        <div>
          <h3>Beskrivelse fra tiltakstype</h3>
          <p>{tiltaksdata?.beskrivelse}</p>
        </div>
        <div>
          <h3>beskrivelse fra gjennomføring</h3>
        </div>
      </div>
    </div>
  );
}
