import { PortableText } from "@portabletext/react";
import sanityClient from "part:@sanity/base/client";
import React, { useEffect, useState } from "react";
import Switch from "react-switch";
import {
  Infoboks,
  Legend,
  PreviewContainer,
  SidemenyDetaljerContainer,
  SidemenyDetaljerRad,
} from "./CommonPreview";

const client = sanityClient.withConfig({ apiVersion: "2021-10-21" });

const tiltaksfarge = "#00347D";
const gjennomforingsfarge = "#881D0C";

export function TiltakstypeOgTiltaksgjennomforingPreview({ document }: any) {
  const [tiltaksdata, setTiltaksdata] = useState(null);
  const [gjennomforingsdata, setGjennomforingsdata] = useState(null);
  const [fargekodet, setFargekodet] = useState(true);
  const { displayed } = document;

  useEffect(() => {
    const fetchData = async () => {
      const data = await client.fetch(
        `*[_type == "tiltakstype" && _id == "${document.displayed.tiltakstype._ref}"]{..., innsatsgruppe->, regelverkLenker[]->}[0]`
      );
      const gjennomforingsdata = await client.fetch(
        `*[_type == "tiltaksgjennomforing" && _id == "${document.displayed._id}"]{..., kontaktinfoArrangor->}[0]`
      );
      setTiltaksdata(data);
      setGjennomforingsdata(gjennomforingsdata);
    };
    fetchData();
  }, [document]);

  function TekstFraTiltakstype({ children }: any) {
    return (
      <span
        title="Tekst hentet fra informasjon om tiltakstypen"
        style={{ color: fargekodet ? tiltaksfarge : "black" }}
      >
        {children}
      </span>
    );
  }

  function TekstFraGjennomforing({ children }: any) {
    return (
      <span
        title="Tekst hentet fra informasjon om tiltaksgjennomføringen"
        style={{ color: fargekodet ? gjennomforingsfarge : "black" }}
      >
        {children}
      </span>
    );
  }

  function InfoboksFraTiltakstype({ children }: any) {
    return (
      <TekstFraTiltakstype>
        <Infoboks>{children}</Infoboks>
      </TekstFraTiltakstype>
    );
  }

  function InfoboksFraGjennomforing({ children }: any) {
    return (
      <TekstFraGjennomforing>
        <Infoboks>{children}</Infoboks>
      </TekstFraGjennomforing>
    );
  }

  function Verktoylinje() {
    return (
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
    );
  }

  function Detaljvisning() {
    return (
      <SidemenyDetaljerContainer>
        {displayed.tiltaksnummer.current && (
          <TekstFraGjennomforing>
            <SidemenyDetaljerRad navn="Tiltaksnummer">
              {displayed.tiltaksnummer.current}
            </SidemenyDetaljerRad>
          </TekstFraGjennomforing>
        )}

        <TekstFraTiltakstype>
          <SidemenyDetaljerRad navn="Tiltakstype">
            {tiltaksdata?.tiltakstypeNavn}
          </SidemenyDetaljerRad>
        </TekstFraTiltakstype>

        {gjennomforingsdata?.kontaktinfoArrangor && (
          <TekstFraGjennomforing>
            <SidemenyDetaljerRad navn="Arrangør">
              {gjennomforingsdata?.kontaktinfoArrangor.selskapsnavn}
            </SidemenyDetaljerRad>
          </TekstFraGjennomforing>
        )}

        <TekstFraTiltakstype>
          <SidemenyDetaljerRad navn="Innsatsgruppe">
            {tiltaksdata?.innsatsgruppe?.beskrivelse}
          </SidemenyDetaljerRad>
        </TekstFraTiltakstype>

        <TekstFraGjennomforing>
          <SidemenyDetaljerRad navn="Oppstart">
            {displayed.oppstart === "dato"
              ? Intl.DateTimeFormat().format(new Date(displayed.oppstartsdato))
              : "Løpende oppstart"}
          </SidemenyDetaljerRad>
        </TekstFraGjennomforing>
        {tiltaksdata?.regelverkLenker && (
          <TekstFraTiltakstype>
            <SidemenyDetaljerRad navn="Regelverk">
              <div
                style={{
                  display: "flex",
                  flexDirection: "column",
                }}
              >
                {tiltaksdata?.regelverkLenker.map((lenke) => {
                  return (
                    <a target="_blank" href={lenke.regelverkUrl}>
                      {lenke.regelverkLenkeNavn}
                    </a>
                  );
                })}
              </div>
            </SidemenyDetaljerRad>
          </TekstFraTiltakstype>
        )}
      </SidemenyDetaljerContainer>
    );
  }

  if (!tiltaksdata && !gjennomforingsdata) return "Laster...";

  return (
    <div style={{ margin: "40px 64px", maxWidth: "600px" }}>
      <Verktoylinje />
      <h1 style={{ borderTop: "1px dotted black", paddingTop: "8px" }}>
        {displayed.tiltaksgjennomforingNavn}
      </h1>
      <Detaljvisning />

      <PreviewContainer>
        <div>
          <h2>Beskrivelse</h2>
          <TekstFraTiltakstype>{tiltaksdata?.beskrivelse}</TekstFraTiltakstype>
          {tiltaksdata?.tiltakstypeNavn === "Opplæring (Gruppe AMO)" && (
            <TekstFraGjennomforing>
              {displayed.beskrivelse}
            </TekstFraGjennomforing>
          )}
        </div>

        <div>
          <h2>For hvem</h2>
          <InfoboksFraTiltakstype>
            {tiltaksdata.faneinnhold?.forHvemInfoboks}
          </InfoboksFraTiltakstype>
          <InfoboksFraGjennomforing>
            {displayed.faneinnhold?.forHvemInfoboks}
          </InfoboksFraGjennomforing>
          <TekstFraTiltakstype>
            <PortableText value={tiltaksdata.faneinnhold?.forHvem} />
          </TekstFraTiltakstype>
          <TekstFraGjennomforing>
            <PortableText value={displayed.faneinnhold?.forHvem} />
          </TekstFraGjennomforing>
        </div>

        <div>
          <h2>Detaljer og innhold</h2>
          <InfoboksFraTiltakstype>
            {tiltaksdata.faneinnhold?.detaljerOgInnholdInfoboks}
          </InfoboksFraTiltakstype>
          <InfoboksFraGjennomforing>
            {displayed.faneinnhold?.detaljerOgInnholdInfoboks}
          </InfoboksFraGjennomforing>
          <TekstFraTiltakstype>
            <PortableText value={tiltaksdata.faneinnhold.detaljerOgInnhold} />
          </TekstFraTiltakstype>
          <TekstFraGjennomforing>
            <PortableText value={displayed.faneinnhold?.detaljerOgInnhold} />
          </TekstFraGjennomforing>
        </div>

        <div>
          <h2>Påmelding og varighet</h2>
          <InfoboksFraTiltakstype>
            {tiltaksdata.faneinnhold?.pameldingOgVarighetInfoboks}
          </InfoboksFraTiltakstype>
          <InfoboksFraGjennomforing>
            {displayed.faneinnhold?.pameldingOgVarighetInfoboks}
          </InfoboksFraGjennomforing>
          <TekstFraTiltakstype>
            <PortableText
              value={tiltaksdata.faneinnhold?.pameldingOgVarighet}
            />
          </TekstFraTiltakstype>
          <TekstFraGjennomforing>
            <PortableText value={displayed.faneinnhold?.pameldingOgVarighet} />
          </TekstFraGjennomforing>
        </div>
      </PreviewContainer>
    </div>
  );
}
