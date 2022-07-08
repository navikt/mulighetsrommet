import React, { PropsWithChildren, ReactNode, useEffect, useState } from "react";
import { useClient } from "sanity";

export function JsonPreview({ document }: any) {
  const client = useClient();

  const [tiltaksdata, setTiltaksdata] = useState<any>(null);

  useEffect(() => {
    const fetchData = async () => {
      const data = await client.fetch(
        `*[_type == "tiltakstype" && _id == "${document.displayed.tiltakstype._ref}"]{..., innsatsgruppe->}[0]`
      );
      setTiltaksdata(data);
    };

    fetchData();
  }, [document]);

  function tilListe(el: any) {
    if (el.listItem === "bullet") {
      const list = (
        <ul>
          {el.children?.map((ch: any, index: number) => {
            return <li key={index}>{ch.text}</li>;
          })}
        </ul>
      );
      return list;
    }

    return el.children?.map((ch: any, index: number) => {
      return (
        <span key={index}>
          {ch.text}
          <br />
        </span>
      );
    });
  }

  if (!tiltaksdata) return "Laster tiltaksdata...";

  const { displayed } = document;
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
            <p>{tiltaksdata.faneinnhold?.forHvem?.map(tilListe)}</p>
          </MinHeight>
          <MinHeight>
            <h3>Detaljer og innhold fra tiltakstype</h3>
            <p>
              {tiltaksdata.faneinnhold.detaljerOgInnhold.map(tilListe)}
            </p>
          </MinHeight>
          <MinHeight>
            <h3>Påmelding og varighet fra tiltakstype</h3>
            <p>{tiltaksdata.faneinnhold?.pameldingOgVarighet?.map(tilListe)}</p>
          </MinHeight>
        </div>
        <div>
          <MinHeight>
            <h3>Beskrivelse fra gjennomføring</h3>
            <p>{displayed.beskrivelse}</p>
          </MinHeight>
          <MinHeight>
            <h3>For hvem fra gjennomføring</h3>
            <p>{displayed.faneinnhold?.forHvem?.map(tilListe)}</p>
          </MinHeight>
          <MinHeight>
            <h3>Detaljer og innhold fra gjennomføring</h3>
            <p>{displayed.faneinnhold?.detaljerOgInnhold?.map(tilListe)}</p>
          </MinHeight>
          <MinHeight>
            <h3>Påmelding og varighet fra gjennomføring</h3>
            <p>{displayed.faneinnhold?.pameldingOgVarighet?.map(tilListe)}</p>
          </MinHeight>
        </div>
      </div>
    </div>
  );
}

function MinHeight({ children }: PropsWithChildren<{}>) {
  return <div style={{ minHeight: "550px" }}>{children}</div>;
}