import { useTiltakstypeFaneinnhold } from "@/api/tiltaksgjennomforing/useTiltakstypeFaneinnhold";
import { Alert, BodyLong, Heading } from "@navikt/ds-react";
import { PortableText } from "@portabletext/react";
import { SanityFaneinnhold } from "mulighetsrommet-api-client";
import { InlineErrorBoundary, LokalInformasjonContainer } from "mulighetsrommet-frontend-common";
import React from "react";
import styles from "../../pages/DetaljerInfo.module.scss";
import { Laster } from "../laster/Laster";
import { Lenkeliste } from "../lenker/Lenkeliste";
import { RedaksjoneltInnholdContainer } from "@/components/redaksjoneltInnhold/RedaksjoneltInnholdContainer";

interface RedaksjoneltInnholdPreviewProps {
  tiltakstypeId: string;
  beskrivelse?: string;
  faneinnhold?: SanityFaneinnhold;
}

export function RedaksjoneltInnholdPreview(props: RedaksjoneltInnholdPreviewProps) {
  return (
    <InlineErrorBoundary>
      <React.Suspense fallback={<Laster tekst="Laster innhold" />}>
        <RedaksjoneltInnhold {...props} />
      </React.Suspense>
    </InlineErrorBoundary>
  );
}

function RedaksjoneltInnhold(props: RedaksjoneltInnholdPreviewProps) {
  const { tiltakstypeId, beskrivelse, faneinnhold } = props;
  const { data: tiltakstypeSanityData } = useTiltakstypeFaneinnhold(tiltakstypeId);
  return (
    <RedaksjoneltInnholdContainer>
      {tiltakstypeSanityData?.beskrivelse && (
        <BodyLong size="large" spacing style={{ whiteSpace: "pre-wrap" }}>
          {tiltakstypeSanityData.beskrivelse}
        </BodyLong>
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
            tiltaksgjennomforing={faneinnhold?.forHvem}
            tiltaksgjennomforingAlert={faneinnhold?.forHvemInfoboks}
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
            tiltaksgjennomforing={faneinnhold?.detaljerOgInnhold}
            tiltaksgjennomforingAlert={faneinnhold?.detaljerOgInnholdInfoboks}
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
            tiltaksgjennomforing={faneinnhold?.pameldingOgVarighet}
            tiltaksgjennomforingAlert={faneinnhold?.pameldingOgVarighetInfoboks}
            tiltakstype={tiltakstypeSanityData?.faneinnhold?.pameldingOgVarighet}
            tiltakstypeAlert={tiltakstypeSanityData?.faneinnhold?.pameldingOgVarighetInfoboks}
          />
        </>
      ) : null}

      {someValuesExists([faneinnhold?.kontaktinfo, faneinnhold?.kontaktinfoInfoboks]) ? (
        <>
          <Heading size="medium">Kontaktinfo</Heading>
          <DetaljerFane
            tiltaksgjennomforing={faneinnhold?.kontaktinfo}
            tiltaksgjennomforingAlert={faneinnhold?.kontaktinfoInfoboks}
          />
        </>
      ) : null}

      {someValuesExists([faneinnhold?.lenker]) ? (
        <>
          <Heading size="medium">Lenker</Heading>
          <Lenkeliste lenker={faneinnhold?.lenker || []} />
        </>
      ) : null}

      {someValuesExists([faneinnhold?.delMedBruker, tiltakstypeSanityData.delingMedBruker]) ? (
        <>
          <Heading size="medium">Del med bruker</Heading>
          <BodyLong as="div" size="small">
            {faneinnhold?.delMedBruker ?? tiltakstypeSanityData.delingMedBruker}
          </BodyLong>
        </>
      ) : null}
    </RedaksjoneltInnholdContainer>
  );
}

function someValuesExists(params: any[]): boolean {
  return params.some((p) => !!p);
}

interface DetaljerFaneProps {
  tiltaksgjennomforingAlert?: string;
  tiltakstypeAlert?: string;
  tiltaksgjennomforing?: any;
  tiltakstype?: any;
}

const DetaljerFane = ({
  tiltaksgjennomforingAlert,
  tiltakstypeAlert,
  tiltaksgjennomforing,
  tiltakstype,
}: DetaljerFaneProps) => {
  if (!tiltaksgjennomforingAlert && !tiltakstypeAlert && !tiltaksgjennomforing && !tiltakstype) {
    return <></>;
  }

  return (
    <div className={styles.faneinnhold_container}>
      {tiltakstype && (
        <>
          <Heading level="2" size="small">
            Generell Informasjon
          </Heading>
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
      {(tiltaksgjennomforing || tiltaksgjennomforingAlert) && (
        <LokalInformasjonContainer>
          <Heading level="2" size="small">
            Lokal Informasjon
          </Heading>
          {tiltaksgjennomforingAlert && (
            <Alert style={{ whiteSpace: "pre-wrap" }} variant="info">
              {tiltaksgjennomforingAlert}
            </Alert>
          )}
          <BodyLong as="div" size="small">
            <PortableText value={tiltaksgjennomforing} />
          </BodyLong>
        </LokalInformasjonContainer>
      )}
    </div>
  );
};
