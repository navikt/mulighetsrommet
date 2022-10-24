import React, { useEffect, useState } from "react";
import sanityClient from "part:@sanity/base/client";
import { Infoboks, MarginBottom, tilListe } from "./common";
const client = sanityClient.withConfig({ apiVersion: "2021-10-21" });

export function TiltakstypePreview({ document }: any) {
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
              {tiltaksdata.faneinnhold.detaljerOgInnhold?.map((el) => {
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
