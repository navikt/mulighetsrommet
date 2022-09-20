import React, { useEffect, useState } from "react";
import sanityClient from "part:@sanity/base/client";

const client = sanityClient.withConfig({ apiVersion: "2021-10-21" });

function MinHeight({ children }) {
  return <div style={{ minHeight: "550px" }}>{children}</div>;
}

export function TiltakstypeOgTiltaksgjennomforingPreview({ document }) {
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
          {el.children?.map((ch, index) => {
            return <li key={index}>{ch.text}</li>;
          })}
        </ul>
      );
      return list;
    }

    return el.children?.map((ch, index) => {
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
              {tiltaksdata.faneinnhold.detaljerOgInnhold.map((el) => {
                return tilListe(el);
              })}
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
