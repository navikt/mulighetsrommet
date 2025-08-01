import { useTiltakstypeFaneinnhold } from "@/api/gjennomforing/useTiltakstypeFaneinnhold";
import { Alert, BodyLong, Heading } from "@navikt/ds-react";
import { PortableText } from "@portabletext/react";
import { EmbeddedTiltakstype, Faneinnhold, Kontorstruktur } from "@mr/api-client-v2";
import { LokalInformasjonContainer } from "@mr/frontend-common";
import React, { Fragment } from "react";
import { Laster } from "../laster/Laster";
import { LenkerList } from "../lenker/LenkerList";
import { RedaksjoneltInnholdContainer } from "@/components/redaksjoneltInnhold/RedaksjoneltInnholdContainer";
import { sorterPaRegionsnavn } from "@/utils/Utils";
import { Metadata } from "../detaljside/Metadata";
import { avtaletekster } from "../ledetekster/avtaleLedetekster";
import { TwoColumnGrid } from "@/layouts/TwoColumGrid";

interface RedaksjoneltInnholdPreviewProps {
  tiltakstype: EmbeddedTiltakstype;
  beskrivelse?: string;
  faneinnhold?: Faneinnhold;
  kontorstruktur: Kontorstruktur;
}

export function RedaksjoneltInnholdPreview(props: RedaksjoneltInnholdPreviewProps) {
  return (
    <React.Suspense fallback={<Laster tekst="Laster innhold" />}>
      <RedaksjoneltInnhold {...props} />
    </React.Suspense>
  );
}
function RedaksjoneltInnhold(props: RedaksjoneltInnholdPreviewProps) {
  const { tiltakstype, beskrivelse, faneinnhold, kontorstruktur } = props;

  const { data: tiltakstypeSanityData } = useTiltakstypeFaneinnhold(tiltakstype.id);
  return (
    <TwoColumnGrid separator>
      <RedaksjoneltInnholdContainer>
        {tiltakstypeSanityData?.beskrivelse && (
          <>
            <Heading size="medium">Generell informasjon</Heading>
            <BodyLong size="large" style={{ whiteSpace: "pre-wrap" }}>
              {tiltakstypeSanityData.beskrivelse}
            </BodyLong>
          </>
        )}
        {beskrivelse && (
          <LokalInformasjonContainer>
            <BodyLong style={{ whiteSpace: "pre-wrap" }} textColor="subtle" size="medium">
              {beskrivelse}
            </BodyLong>
          </LokalInformasjonContainer>
        )}
        {someValuesExists([
          faneinnhold?.forHvem,
          faneinnhold?.forHvemInfoboks,
          tiltakstypeSanityData?.faneinnhold?.forHvem,
          tiltakstypeSanityData?.faneinnhold?.forHvemInfoboks,
        ]) ? (
          <>
            <Heading size="medium">For hvem</Heading>
            <DetaljerFane
              gjennomforing={faneinnhold?.forHvem}
              gjennomforingAlert={faneinnhold?.forHvemInfoboks}
              tiltakstype={tiltakstypeSanityData?.faneinnhold?.forHvem}
              tiltakstypeAlert={tiltakstypeSanityData?.faneinnhold?.forHvemInfoboks}
            />
          </>
        ) : null}

        {someValuesExists([
          faneinnhold?.detaljerOgInnhold,
          faneinnhold?.detaljerOgInnholdInfoboks,
          tiltakstypeSanityData?.faneinnhold?.detaljerOgInnhold,
          tiltakstypeSanityData?.faneinnhold?.detaljerOgInnholdInfoboks,
        ]) ? (
          <>
            <Heading size="medium">Detaljer og innhold</Heading>
            <DetaljerFane
              gjennomforing={faneinnhold?.detaljerOgInnhold}
              gjennomforingAlert={faneinnhold?.detaljerOgInnholdInfoboks}
              tiltakstype={tiltakstypeSanityData?.faneinnhold?.detaljerOgInnhold}
              tiltakstypeAlert={tiltakstypeSanityData?.faneinnhold?.detaljerOgInnholdInfoboks}
            />
          </>
        ) : null}

        {someValuesExists([
          faneinnhold?.pameldingOgVarighet,
          faneinnhold?.pameldingOgVarighetInfoboks,
          tiltakstypeSanityData?.faneinnhold?.pameldingOgVarighet,
          tiltakstypeSanityData?.faneinnhold?.pameldingOgVarighetInfoboks,
        ]) ? (
          <>
            <Heading size="medium">Påmelding og varighet</Heading>
            <DetaljerFane
              gjennomforing={faneinnhold?.pameldingOgVarighet}
              gjennomforingAlert={faneinnhold?.pameldingOgVarighetInfoboks}
              tiltakstype={tiltakstypeSanityData?.faneinnhold?.pameldingOgVarighet}
              tiltakstypeAlert={tiltakstypeSanityData?.faneinnhold?.pameldingOgVarighetInfoboks}
            />
          </>
        ) : null}

        {someValuesExists([faneinnhold?.kontaktinfo, faneinnhold?.kontaktinfoInfoboks]) ? (
          <>
            <Heading size="medium">Kontaktinfo</Heading>
            <DetaljerFane
              gjennomforing={faneinnhold?.kontaktinfo}
              gjennomforingAlert={faneinnhold?.kontaktinfoInfoboks}
            />
          </>
        ) : null}

        {someValuesExists([faneinnhold?.lenker]) ? (
          <div className="prose">
            <Heading size="medium">Lenker</Heading>
            <LenkerList lenker={faneinnhold?.lenker || []} />
          </div>
        ) : null}

        {someValuesExists([faneinnhold?.delMedBruker, tiltakstypeSanityData.delingMedBruker]) ? (
          <>
            <Heading size="medium">Del med bruker</Heading>
            <BodyLong as="div" size="small" className="prose">
              {faneinnhold?.delMedBruker ?? tiltakstypeSanityData.delingMedBruker}
            </BodyLong>
          </>
        ) : null}
      </RedaksjoneltInnholdContainer>
      <RedaksjoneltInnholdContainer>
        <Heading size="medium" level="3">
          Geografisk tilgjengelighet
        </Heading>
        {kontorstruktur.length > 1 ? (
          <Metadata
            header={avtaletekster.fylkessamarbeidLabel}
            verdi={
              <ul>
                {kontorstruktur.sort(sorterPaRegionsnavn).map((kontor) => {
                  return <li key={kontor.region.enhetsnummer}>{kontor.region.navn}</li>;
                })}
              </ul>
            }
          />
        ) : (
          kontorstruktur.map((struktur, index) => {
            return (
              <Fragment key={index}>
                <Metadata header={avtaletekster.navRegionerLabel} verdi={struktur.region.navn} />

                <Metadata
                  header={avtaletekster.navEnheterLabel}
                  verdi={
                    <ul className="columns-2">
                      {struktur.kontorer.map((kontor) => (
                        <li key={kontor.enhetsnummer}>{kontor.navn}</li>
                      ))}
                    </ul>
                  }
                />
              </Fragment>
            );
          })
        )}
      </RedaksjoneltInnholdContainer>
    </TwoColumnGrid>
  );
}

function someValuesExists(params: any[]): boolean {
  return params.some((p) => !!p);
}

interface DetaljerFaneProps {
  gjennomforingAlert?: string;
  tiltakstypeAlert?: string;
  gjennomforing?: any;
  tiltakstype?: any;
}

const DetaljerFane = ({
  gjennomforingAlert,
  tiltakstypeAlert,
  gjennomforing,
  tiltakstype,
}: DetaljerFaneProps) => {
  if (!gjennomforingAlert && !tiltakstypeAlert && !gjennomforing && !tiltakstype) {
    return <></>;
  }

  return (
    <div>
      {tiltakstype && (
        <>
          {tiltakstypeAlert && (
            <Alert style={{ whiteSpace: "pre-wrap" }} variant="info">
              {tiltakstypeAlert}
            </Alert>
          )}
          <BodyLong as="div" size="small">
            <PortableText value={tiltakstype} />
          </BodyLong>
        </>
      )}
      {(gjennomforing || gjennomforingAlert) && (
        <LokalInformasjonContainer>
          <Heading level="2" size="small" spacing className="mt-0">
            Lokal Informasjon
          </Heading>
          {gjennomforingAlert && (
            <Alert style={{ whiteSpace: "pre-wrap" }} variant="info">
              {gjennomforingAlert}
            </Alert>
          )}
          <BodyLong as="div" size="small">
            <PortableText value={gjennomforing} />
          </BodyLong>
        </LokalInformasjonContainer>
      )}
    </div>
  );
};
