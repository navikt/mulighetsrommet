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

export const getDefaultDocumentNode = ({ schemaType }) => {
  if (schemaType === "tiltaksgjennomforing") {
    return S.document().views([
      S.view.form(),
      S.view.component(JsonPreview).title("Gjennomføring med tiltakstype"),
    ]);
  }
};

function MinHeight({ children }) {
  return <div style={{ minHeight: "550px" }}>{children}</div>;
}

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

  function tilListe(el) {
    if (el.listItem === "bullet") {
      const list = (
        <ul>
          {el.children.map((ch) => {
            return <li>{ch.text}</li>;
          })}
        </ul>
      );
      return list;
    }

    return el.children.map((ch) => {
      return (
        <span>
          {ch.text}
          <br />
        </span>
      );
    });
  }

  if (!tiltaksdata) return "Laster tiltaksdata...";

  const { displayed } = document;
  console.log({ tiltaksdata });
  return (
    <div style={{ padding: "20px" }}>
      <h1>{displayed.tiltaksgjennomforingNavn}</h1>
      <small>Tiltakstype: {tiltaksdata.tiltakstypeNavn}</small>
      <div
        style={{
          display: "grid",
          gap: "20px",
          gridTemplateColumns: "repeat(2, 1fr)",
        }}
      >
        <div>
          <MinHeight>
            <h3>Beskrivelse fra tiltakstype</h3>
            <p>{tiltaksdata?.beskrivelse}</p>
          </MinHeight>
          <MinHeight>
            <h3>For hvem fra tiltakstype</h3>
            <p>{tiltaksdata.faneinnhold.forHvem.map(tilListe)}</p>
          </MinHeight>
          <MinHeight>
            <h3>Detaljer og innhold fra tiltakstype</h3>
            <p>
              {tiltaksdata.faneinnhold.detaljerOgInnhold.map((el) => {
                return tilListe(el);
              })}
            </p>
          </MinHeight>
          <MinHeight>
            <h3>Påmelding og varighet fra tiltakstype</h3>
            <p>{tiltaksdata.faneinnhold.pameldingOgVarighet.map(tilListe)}</p>
          </MinHeight>
        </div>
        <div>
          <MinHeight>
            <h3>Beskrivelse fra gjennomføring</h3>
            <p>{displayed.beskrivelse}</p>
          </MinHeight>
          <MinHeight>
            <h3>For hvem fra gjennomføring</h3>
            <p>{displayed.faneinnhold.forHvem.map(tilListe)}</p>
          </MinHeight>
          <MinHeight>
            <h3>Detaljer og innhold fra gjennomføring</h3>
            <p>{displayed.faneinnhold.detaljerOgInnhold.map(tilListe)}</p>
          </MinHeight>
          <MinHeight>
            <h3>Påmelding og varighet fra gjennomføring</h3>
            <p>{displayed.faneinnhold.pameldingOgVarighet.map(tilListe)}</p>
          </MinHeight>
        </div>
      </div>
    </div>
  );
}
