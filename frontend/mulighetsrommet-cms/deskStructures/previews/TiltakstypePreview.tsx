import React, { useEffect, useState } from "react";
import sanityClient from "part:@sanity/base/client";
import {
  Infoboks,
  PreviewContainer,
  SidemenyDetaljerContainer,
  SidemenyDetaljerRad,
} from "./common";
import { PortableText } from "@portabletext/react";

const client = sanityClient.withConfig({ apiVersion: "2021-10-21" });

export function TiltakstypePreview({ document }: any) {
  const [tiltaksdata, setTiltaksdata] = useState(null);
  const { displayed } = document;

  useEffect(() => {
    const fetchData = async () => {
      const data = await client.fetch(
        `*[_type == "tiltakstype" && _id == "${document.displayed._id}"]{..., innsatsgruppe->, regelverkLenker[]->}[0]`
      );
      setTiltaksdata(data);
    };
    fetchData();
  }, [document]);

  function TekstFraTiltakstype({ children }: any) {
    return <span>{children}</span>;
  }

  function Detaljvisning() {
    return (
      <SidemenyDetaljerContainer>
        <SidemenyDetaljerRad navn="Tiltaksnummer">
          (Kommer fra tiltaksgjennomføringene)
        </SidemenyDetaljerRad>
        <SidemenyDetaljerRad navn="Tiltakstype">
          {tiltaksdata?.tiltakstypeNavn}
        </SidemenyDetaljerRad>
        <SidemenyDetaljerRad navn="Arrangør">
          (Kommer fra tiltaksgjennomføringene)
        </SidemenyDetaljerRad>
        <SidemenyDetaljerRad navn="Innsatsgruppe">
          {tiltaksdata?.innsatsgruppe?.beskrivelse}
        </SidemenyDetaljerRad>
        <SidemenyDetaljerRad navn="Oppstart">
          (Kommer fra tiltaksgjennomføringene)
        </SidemenyDetaljerRad>
        {tiltaksdata?.regelverkLenker && (
          <SidemenyDetaljerRad navn="Regelverk">
            <div
              style={{
                display: "flex",
                flexDirection: "column",
              }}
            >
              {tiltaksdata?.regelverkLenker.map((lenke) => {
                return (
                  <a href={lenke.regelverkUrl}>{lenke.regelverkLenkeNavn}</a>
                );
              })}
            </div>
          </SidemenyDetaljerRad>
        )}
      </SidemenyDetaljerContainer>
    );
  }

  if (!tiltaksdata) return "Laster...";

  return (
    <div style={{ margin: "0 64px", maxWidth: "600px" }}>
      <h1>{tiltaksdata.tiltakstypeNavn}</h1>
      <Detaljvisning />

      <PreviewContainer>
        <div>
          <h2>Beskrivelse</h2>
          {tiltaksdata?.beskrivelse}
        </div>

        {(tiltaksdata.faneinnhold?.forHvem ||
          displayed.faneinnhold?.forHvemInfoboks) && (
          <div>
            <h2>For hvem</h2>
            <Infoboks>{displayed.faneinnhold?.forHvemInfoboks}</Infoboks>
            <PortableText value={tiltaksdata.faneinnhold?.forHvem} />
          </div>
        )}

        {(displayed.faneinnhold?.detaljerOgInnholdInfoboks ||
          tiltaksdata.faneinnhold.detaljerOgInnhold) && (
          <div>
            <h2>Detaljer og innhold</h2>
            <Infoboks>
              {displayed.faneinnhold?.detaljerOgInnholdInfoboks}
            </Infoboks>
            <PortableText value={tiltaksdata.faneinnhold.detaljerOgInnhold} />
          </div>
        )}

        {(displayed.faneinnhold?.pameldingOgVarighetInfoboks ||
          tiltaksdata.faneinnhold?.pameldingOgVarighet) && (
          <div>
            <h2>Påmelding og varighet</h2>
            <Infoboks>
              {displayed.faneinnhold?.pameldingOgVarighetInfoboks}
            </Infoboks>
            <PortableText
              value={tiltaksdata.faneinnhold?.pameldingOgVarighet}
            />
          </div>
        )}
      </PreviewContainer>
    </div>
  );
}
