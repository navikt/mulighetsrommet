import { PortableText } from "@portabletext/react";
import sanityClient from "part:@sanity/base/client";
import React, { useEffect, useState } from "react";
import Switch from "react-switch";
import { Infoboks, Legend, MarginBottom } from "./common";

const client = sanityClient.withConfig({ apiVersion: "2021-10-21" });

const tiltaksfarge = "#00347D";
const gjennomforingsfarge = "#881D0C";

export function TiltakstypeOgTiltaksgjennomforingPreview({ document }: any) {
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

  function TekstFraTiltakstype({ children }: any) {
    return (
      <p
        title="Tekst hentet fra informasjon om tiltakstypen"
        style={{ color: fargekodet ? tiltaksfarge : "black" }}
      >
        {children}
      </p>
    );
  }

  function TekstFraGjennomforing({ children }: any) {
    return (
      <p
        title="Tekst hentet fra informasjon om tiltaksgjennomføringen"
        style={{ color: fargekodet ? gjennomforingsfarge : "black" }}
      >
        {children}
      </p>
    );
  }

  function Verktoylinje() {
    return (
      <>
        <div
          style={{
            display: "flex",
            height: "20px",
            alignItems: "center",
          }}
        >
          <div
            style={{
              display: "flex",
              alignItems: "center",
            }}
          >
            <small style={{ marginRight: "4px" }}>
              Marker tekst fra tiltakstype og tiltaksgjennomføring
            </small>
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
            <div
              style={{
                marginLeft: "16px",
              }}
            >
              <div>
                <Legend farge={tiltaksfarge}>Fra tiltakstype</Legend>
              </div>
              <Legend farge={gjennomforingsfarge}>
                Fra tiltaksgjennomføring
              </Legend>
            </div>
          )}
        </div>
      </>
    );
  }

  if (!tiltaksdata) return "Laster tiltaksdata...";

  const { displayed } = document;
  return (
    <div style={{ margin: "64px" }}>
      <Verktoylinje />
      <div style={{ maxWidth: "600px" }}>
        <div style={{ display: "flex", justifyContent: "space-between" }}>
          <h1 style={{ borderTop: "1px dotted black", paddingTop: "8px" }}>
            {displayed.tiltaksgjennomforingNavn}
          </h1>
        </div>
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
            <TekstFraTiltakstype>
              {tiltaksdata?.beskrivelse}
            </TekstFraTiltakstype>
            {tiltaksdata?.tiltakstypeNavn === "Opplæring (Gruppe AMO)" && (
              <TekstFraGjennomforing>
                {displayed.beskrivelse}
              </TekstFraGjennomforing>
            )}
          </MarginBottom>
          <MarginBottom>
            <h3>For hvem</h3>
            <Infoboks>{tiltaksdata.faneinnhold?.forHvemInfoboks}</Infoboks>
            <Infoboks>{displayed.faneinnhold?.forHvemInfoboks}</Infoboks>
            <TekstFraTiltakstype>
              <PortableText value={tiltaksdata.faneinnhold?.forHvem} />
            </TekstFraTiltakstype>
            <TekstFraGjennomforing>
              <PortableText value={displayed.faneinnhold?.forHvem} />
            </TekstFraGjennomforing>
          </MarginBottom>
          <MarginBottom>
            <h3>Detaljer og innhold</h3>
            <Infoboks>
              {tiltaksdata.faneinnhold?.detaljerOgInnholdInfoboks}
            </Infoboks>
            <Infoboks>
              {displayed.faneinnhold?.detaljerOgInnholdInfoboks}
            </Infoboks>
            <TekstFraTiltakstype>
              <PortableText value={tiltaksdata.faneinnhold.detaljerOgInnhold} />
            </TekstFraTiltakstype>
            <TekstFraGjennomforing>
              <PortableText value={displayed.faneinnhold?.detaljerOgInnhold} />
            </TekstFraGjennomforing>
          </MarginBottom>
          <MarginBottom>
            <h3>Påmelding og varighet</h3>
            <Infoboks>
              {tiltaksdata.faneinnhold?.pameldingOgVarighetInfoboks}
            </Infoboks>
            <Infoboks>
              {displayed.faneinnhold?.pameldingOgVarighetInfoboks}
            </Infoboks>
            <TekstFraTiltakstype>
              <PortableText
                value={tiltaksdata.faneinnhold?.pameldingOgVarighet}
              />
            </TekstFraTiltakstype>
            <TekstFraGjennomforing>
              <PortableText
                value={displayed.faneinnhold?.pameldingOgVarighet}
              />
            </TekstFraGjennomforing>
          </MarginBottom>
        </div>
      </div>
    </div>
  );
}
