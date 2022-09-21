import React, { useEffect, useState } from "react";
import sanityClient from "part:@sanity/base/client";
import Switch from "react-switch";

const client = sanityClient.withConfig({ apiVersion: "2021-10-21" });

function MarginBottom({ children }) {
  return <div style={{ marginBottom: "4rem" }}>{children}</div>;
}

const tekstMarkeringsFarge = '#00347D'
const gjennomforingsfarge = '#881D0C'

export function TiltaksgjennomforingOgTypePreview({ document }) {
  const [tiltaksdata, setTiltaksdata] = useState(null);
  const [fargekodet, setFargekodet] = useState(true);

  useEffect(() => {
    const fetchData = async () => {
      const data = await client.fetch(
        `*[_type == "tiltakstype" && _id == "${document.displayed.tiltakstype._ref}"]{..., innsatsgruppe->}[0]`
      );
      setTiltaksdata(data);
    };

    fetchData();
  }, [document]);

  function TekstFraTiltakstype({ children }) {
    return (
      <p style={{ color: fargekodet ? tekstMarkeringsFarge : "black" }}>{children}</p>
    );
  }

  function TekstFraGjennomforing({ children }) {
    return (
      <p style={{ color: fargekodet ? gjennomforingsfarge : "black" }}>{children}</p>
    );
  }

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
      <div style={{ display: "flex", justifyContent: "space-between" }}>
        <small>Tiltakstype: {tiltaksdata.tiltakstypeNavn}</small>
        <div>
          <medium>Marker tekst fra tiltakstype og tiltaksgjennomføring i ulike farger</medium>
          <Switch
            onChange={() => setFargekodet(!fargekodet)}
            checked={fargekodet}
            uncheckedIcon={false}
            checkedIcon={false}
            onColor={tekstMarkeringsFarge}
          />
          <TekstFraTiltakstype>Tekst fra tiltakstype</TekstFraTiltakstype>
          <TekstFraGjennomforing>Tekst fra tiltaksgjennomføring</TekstFraGjennomforing>
        </div>
      </div>
      <div>
        <MarginBottom>
          <h3>Beskrivelse</h3>
          {tiltaksdata?.tiltakstypeNavn === "Opplæring (Gruppe AMO)" && (
            <TekstFraGjennomforing>{displayed.beskrivelse}</TekstFraGjennomforing>
          )}
          <TekstFraTiltakstype>{tiltaksdata?.beskrivelse}</TekstFraTiltakstype>
        </MarginBottom>
        <MarginBottom>
          <h3>For hvem</h3>
          <TekstFraTiltakstype>
            {tiltaksdata.faneinnhold?.forHvem?.map(tilListe)}
          </TekstFraTiltakstype>
          <TekstFraGjennomforing>{displayed.faneinnhold?.forHvem?.map(tilListe)}</TekstFraGjennomforing>
        </MarginBottom>
        <MarginBottom>
          <h3>Detaljer og innhold</h3>
          <TekstFraTiltakstype>
            {tiltaksdata.faneinnhold.detaljerOgInnhold.map((el) => {
              return tilListe(el);
            })}
          </TekstFraTiltakstype>
          <TekstFraGjennomforing>{displayed.faneinnhold?.detaljerOgInnhold?.map(tilListe)}</TekstFraGjennomforing>
        </MarginBottom>
        <MarginBottom>
          <h3>Påmelding og varighet</h3>
          <TekstFraTiltakstype>
            {tiltaksdata.faneinnhold?.pameldingOgVarighet?.map(tilListe)}
          </TekstFraTiltakstype>
          <TekstFraGjennomforing>{displayed.faneinnhold?.pameldingOgVarighet?.map(tilListe)}</TekstFraGjennomforing>
        </MarginBottom>
      </div>
    </div>
  );
}
