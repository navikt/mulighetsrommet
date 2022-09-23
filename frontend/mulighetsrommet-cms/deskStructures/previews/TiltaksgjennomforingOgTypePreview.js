import React, { useEffect, useState } from "react";
import sanityClient from "part:@sanity/base/client";
import Switch from "react-switch";
import ReactColorSquare from "react-color-square";

const client = sanityClient.withConfig({ apiVersion: "2021-10-21" });

function MarginBottom({ children }) {
  return <div style={{ marginBottom: "4rem" }}>{children}</div>;
}

function Legend({ farge, children }) {
  return (
    <div style={{ display: "flex", alignItems: "center" }}>
      <ReactColorSquare height={12} width={12} color={farge} />
      <small style={{ marginLeft: "4px" }}>{children}</small>
    </div>
  );
}

const tiltaksfarge = "#00347D";
const gjennomforingsfarge = "#881D0C";

export function TiltaksgjennomforingOgTypePreview({ document }) {
  const [tiltaksdata, setTiltaksdata] = useState(null);
  const [fargekodet, setFargekodet] = useState(false);

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
      <p style={{ color: fargekodet ? tiltaksfarge : "black" }}>{children}</p>
    );
  }

  function TekstFraGjennomforing({ children }) {
    return (
      <p style={{ color: fargekodet ? gjennomforingsfarge : "black" }}>
        {children}
      </p>
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

  function Verktoylinje() {
    return (
      <div
        style={{
          display: "flex",
          justifyContent: "space-between",
          alignItems: "center",
          height: "20px",
        }}
      >
        <div
          style={{
            display: "flex",
            justifyContent: "space-between",
            alignItems: "center",
          }}
        >
          <medium style={{ marginRight: "4px" }}>
            Marker tekst fra tiltakstype og tiltaksgjennomføring i ulike farger
          </medium>
          <Switch
            onChange={() => setFargekodet(!fargekodet)}
            checked={fargekodet}
            uncheckedIcon={false}
            checkedIcon={false}
            onColor={tiltaksfarge}
            height={20}
            width={36}
          />
        </div>
        {fargekodet && (
          <div>
            <Legend farge={tiltaksfarge}>Tekst fra tiltakstype</Legend>
            <Legend farge={gjennomforingsfarge}>
              Tekst fra tiltaksgjennomføring
            </Legend>
          </div>
        )}
      </div>
    );
  }

  if (!tiltaksdata) return "Laster tiltaksdata...";

  const { displayed } = document;
  return (
    <div style={{ margin: "64px" }}>
      <Verktoylinje />
      <div style={{ maxWidth: "600px" }}>
        <h1 style={{ borderTop: "1px dotted black", paddingTop: "8px" }}>
          {displayed.tiltaksgjennomforingNavn}
        </h1>
        <div style={{ display: "flex", justifyContent: "space-between" }}>
          <small style={{ paddingTop: "4px", paddingBottom: "4px" }}>
            Tiltakstype: {tiltaksdata.tiltakstypeNavn}
          </small>
          <small style={{ border: "1px dashed black", padding: "4px" }}>
            Boks med nøkkelinformasjon vises ikke i denne forhåndsvisningen
          </small>
        </div>
        <div>
          <MarginBottom>
            <h3>Beskrivelse</h3>
            {tiltaksdata?.tiltakstypeNavn === "Opplæring (Gruppe AMO)" && (
              <TekstFraGjennomforing>
                {displayed.beskrivelse}
              </TekstFraGjennomforing>
            )}
            <TekstFraTiltakstype>
              {tiltaksdata?.beskrivelse}
            </TekstFraTiltakstype>
          </MarginBottom>
          <MarginBottom>
            <h3>For hvem</h3>
            <TekstFraTiltakstype>
              {tiltaksdata.faneinnhold?.forHvem?.map(tilListe)}
            </TekstFraTiltakstype>
            <TekstFraGjennomforing>
              {displayed.faneinnhold?.forHvem?.map(tilListe)}
            </TekstFraGjennomforing>
          </MarginBottom>
          <MarginBottom>
            <h3>Detaljer og innhold</h3>
            <TekstFraTiltakstype>
              {tiltaksdata.faneinnhold.detaljerOgInnhold.map((el) => {
                return tilListe(el);
              })}
            </TekstFraTiltakstype>
            <TekstFraGjennomforing>
              {displayed.faneinnhold?.detaljerOgInnhold?.map(tilListe)}
            </TekstFraGjennomforing>
          </MarginBottom>
          <MarginBottom>
            <h3>Påmelding og varighet</h3>
            <TekstFraTiltakstype>
              {tiltaksdata.faneinnhold?.pameldingOgVarighet?.map(tilListe)}
            </TekstFraTiltakstype>
            <TekstFraGjennomforing>
              {displayed.faneinnhold?.pameldingOgVarighet?.map(tilListe)}
            </TekstFraGjennomforing>
          </MarginBottom>
        </div>
      </div>
    </div>
  );
}
