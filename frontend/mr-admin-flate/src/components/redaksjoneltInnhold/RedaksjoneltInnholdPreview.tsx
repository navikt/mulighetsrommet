import { useTiltakstypeFaneinnhold } from "@/api/gjennomforing/useTiltakstypeFaneinnhold";
import { Alert, BodyLong, Heading } from "@navikt/ds-react";
import { PortableText } from "@portabletext/react";
import { EmbeddedTiltakstype, Faneinnhold } from "@mr/api-client";
import { LokalInformasjonContainer } from "@mr/frontend-common";
import React from "react";
import styles from "../../pages/DetaljerInfo.module.scss";
import { Laster } from "../laster/Laster";
import { Lenkeliste } from "../lenker/Lenkeliste";
import { RedaksjoneltInnholdContainer } from "@/components/redaksjoneltInnhold/RedaksjoneltInnholdContainer";
import { InlineErrorBoundary } from "@/ErrorBoundary";

interface RedaksjoneltInnholdPreviewProps {
  tiltakstype: EmbeddedTiltakstype;
  beskrivelse?: string;
  faneinnhold?: Faneinnhold;
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
  const { tiltakstype, beskrivelse, faneinnhold } = props;
  const { data: tiltakstypeSanityData } = useTiltakstypeFaneinnhold(tiltakstype.id);
  return (
    <div className="prose prose-headings:mb-0 min-w-1/2">
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
    </div>
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
    <div className={styles.faneinnhold_container}>
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
