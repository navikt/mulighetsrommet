import { SanityFaneinnhold } from "mulighetsrommet-api-client";
import styles from "../../pages/DetaljerInfo.module.scss";
import { Alert, BodyLong, Heading } from "@navikt/ds-react";
import { useTiltakstypeFaneinnhold } from "../../api/tiltaksgjennomforing/useTiltakstypeFaneinnhold";
import { PortableText } from "@portabletext/react";
import { InlineErrorBoundary } from "../../ErrorBoundary";
import React from "react";
import { Laster } from "../laster/Laster";

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
    <div className={styles.red_innhold_container}>
      {tiltakstypeSanityData?.beskrivelse && (
        <BodyLong size="large" className={styles.preWrap}>
          {tiltakstypeSanityData.beskrivelse}
        </BodyLong>
      )}
      {beskrivelse && (
        <div className={styles.lokal_informasjon}>
          <BodyLong className={styles.preWrap} textColor="subtle" size="medium">
            {beskrivelse}
          </BodyLong>
        </div>
      )}
      <Heading size="medium">For hvem</Heading>
      <DetaljerFane
        tiltaksgjennomforing={faneinnhold?.forHvem}
        tiltaksgjennomforingAlert={faneinnhold?.forHvemInfoboks}
        tiltakstype={tiltakstypeSanityData?.faneinnhold?.forHvem}
        tiltakstypeAlert={tiltakstypeSanityData?.faneinnhold?.forHvemInfoboks}
      />
      <Heading size="medium">Detaljer og innhold</Heading>
      <DetaljerFane
        tiltaksgjennomforing={faneinnhold?.detaljerOgInnhold}
        tiltaksgjennomforingAlert={faneinnhold?.detaljerOgInnholdInfoboks}
        tiltakstype={tiltakstypeSanityData?.faneinnhold?.detaljerOgInnhold}
        tiltakstypeAlert={tiltakstypeSanityData?.faneinnhold?.detaljerOgInnholdInfoboks}
      />
      <Heading size="medium">Påmelding og varighet</Heading>
      <DetaljerFane
        tiltaksgjennomforing={faneinnhold?.pameldingOgVarighet}
        tiltaksgjennomforingAlert={faneinnhold?.pameldingOgVarighetInfoboks}
        tiltakstype={tiltakstypeSanityData?.faneinnhold?.pameldingOgVarighet}
        tiltakstypeAlert={tiltakstypeSanityData?.faneinnhold?.pameldingOgVarighetInfoboks}
      />
      <Heading size="medium">Kontaktinfo</Heading>
      <DetaljerFane
        tiltaksgjennomforing={faneinnhold?.kontaktinfo}
        tiltaksgjennomforingAlert={faneinnhold?.kontaktinfoInfoboks}
      />
      <Heading size="medium">Del med bruker</Heading>
      <BodyLong as="div" size="small">
        {faneinnhold?.delMedBruker ?? tiltakstypeSanityData.delingMedBruker}
      </BodyLong>
    </div>
  );
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
            <Alert className={styles.preWrap} variant="info">
              {tiltakstypeAlert}
            </Alert>
          )}
          <BodyLong as="div" size="small">
            <PortableText value={tiltakstype} />
          </BodyLong>
        </>
      )}
      {(tiltaksgjennomforing || tiltaksgjennomforingAlert) && (
        <div className={styles.lokal_informasjon}>
          <Heading level="2" size="small">
            Lokal Informasjon
          </Heading>
          {tiltaksgjennomforingAlert && (
            <Alert className={styles.preWrap} variant="info">
              {tiltaksgjennomforingAlert}
            </Alert>
          )}
          <BodyLong as="div" size="small">
            <PortableText value={tiltaksgjennomforing} />
          </BodyLong>
        </div>
      )}
    </div>
  );
};
