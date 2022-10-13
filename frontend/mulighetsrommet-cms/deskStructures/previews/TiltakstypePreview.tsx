import React, { useEffect, useState } from "react";
import sanityClient from "part:@sanity/base/client";
import { GrCircleInformation } from "react-icons/gr";
const client = sanityClient.withConfig({ apiVersion: "2021-10-21" });

function Legend({ farge, children }) {
  return (
    <div style={{ display: "flex", alignItems: "center" }}>
      <small style={{ marginLeft: "4px", textAlign: "right" }}>
        {children}
      </small>
    </div>
  );
}

function MarginBottom({ children }) {
  return <div style={{ marginBottom: "4rem" }}>{children}</div>;
}

function Infoboks({ children }) {
  if (!children) return null;

  return (
    <div
      style={{
        backgroundColor: "#ebfcff",
        border: "1px solid black",
        display: "flex",
        alignItems: "baseline",
        gap: "10px",
        padding: "0px 10px",
      }}
    >
      <GrCircleInformation />
      <p>{children}</p>
    </div>
  );
}

export function TiltakstypePreview({ document }) {
  const [tiltaksdata, setTiltaksdata] = useState(null);

  useEffect(() => {
    const fetchData = async () => {
      const data = await client.fetch(
        `*[_type == "tiltakstype" && _id == "${document.displayed._id}"]{..., innsatsgruppe->}[0]`
      );
      setTiltaksdata(data);
    };

    fetchData();
  }, [document]);

  function TekstFraTiltakstype({ children }) {
    return <p>{children}</p>;
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
    <div style={{ margin: "64px" }}>
      <div style={{ maxWidth: "600px" }}>
        <div style={{ display: "flex", justifyContent: "space-between" }}>
          <h1 style={{ borderTop: "1px dotted black", paddingTop: "8px" }}>
            {displayed.tiltaksgjennomforingNavn}
          </h1>
        </div>
        <div style={{ display: "flex", justifyContent: "space-between" }}>
          <p style={{ paddingTop: "4px", paddingBottom: "4px" }}>
            Tiltakstype: {tiltaksdata.tiltakstypeNavn}
          </p>
          <small style={{ border: "1px dashed black", padding: "4px" }}>
            Boks med nøkkelinformasjon vises ikke i denne forhåndsvisningen
          </small>
        </div>
        <div>
          <MarginBottom>
            <h3>Beskrivelse</h3>
            <TekstFraTiltakstype>
              {tiltaksdata?.beskrivelse}
            </TekstFraTiltakstype>
          </MarginBottom>
          <MarginBottom>
            <h3>For hvem</h3>
            <Infoboks>{displayed.faneinnhold?.forHvemInfoboks}</Infoboks>
            <TekstFraTiltakstype>
              {tiltaksdata.faneinnhold?.forHvem?.map(tilListe)}
            </TekstFraTiltakstype>
          </MarginBottom>
          <MarginBottom>
            <h3>Detaljer og innhold</h3>
            <Infoboks>
              {displayed.faneinnhold?.detaljerOgInnholdInfoboks}
            </Infoboks>
            <TekstFraTiltakstype>
              {tiltaksdata.faneinnhold.detaljerOgInnhold.map((el) => {
                return tilListe(el);
              })}
            </TekstFraTiltakstype>
          </MarginBottom>
          <MarginBottom>
            <h3>Påmelding og varighet</h3>
            <Infoboks>
              {displayed.faneinnhold?.pameldingOgVarighetInfoboks}
            </Infoboks>
            <TekstFraTiltakstype>
              {tiltaksdata.faneinnhold?.pameldingOgVarighet?.map(tilListe)}
            </TekstFraTiltakstype>
          </MarginBottom>
        </div>
      </div>
    </div>
  );
}
