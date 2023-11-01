import { Tiltaksgjennomforing } from "mulighetsrommet-api-client";
import styles from "../DetaljerInfo.module.scss";
import { Alert, BodyLong, Heading } from "@navikt/ds-react";
import { useTiltakstypeSanityData } from "../../api/tiltaksgjennomforing/useTiltakstypeSanityData";
import { PortableText } from "@portabletext/react";

interface Props {
  tiltaksgjennomforing: Tiltaksgjennomforing;
}

export const TiltaksgjennomforingRedInnhold = (props: Props) => {
  const { tiltaksgjennomforing } = props;
  const { data: tiltakstypeSanityData } = useTiltakstypeSanityData(
    tiltaksgjennomforing.tiltakstype.id,
  );

  return (
    <div className={styles.red_innhold_container}>
      {tiltakstypeSanityData?.beskrivelse && (
        <BodyLong size="large" className={styles.preWrap}>
          {tiltakstypeSanityData.beskrivelse}
        </BodyLong>
      )}
      {tiltaksgjennomforing.beskrivelse && (
        <div className={styles.lokal_informasjon}>
          <BodyLong className={styles.preWrap} textColor="subtle" size="medium">
            {tiltaksgjennomforing.beskrivelse}
          </BodyLong>
        </div>
      )}
      <Heading size="medium">For hvem</Heading>
      <DetaljerFane
        tiltaksgjennomforing={tiltaksgjennomforing.faneinnhold?.forHvem}
        tiltaksgjennomforingAlert={tiltaksgjennomforing.faneinnhold?.forHvemInfoboks}
        tiltakstype={tiltakstypeSanityData?.faneinnhold?.forHvem}
        tiltakstypeAlert={tiltakstypeSanityData?.faneinnhold?.forHvemInfoboks}
      />
      <Heading size="medium">Detaljer og innhold</Heading>
      <DetaljerFane
        tiltaksgjennomforing={tiltaksgjennomforing.faneinnhold?.detaljerOgInnhold}
        tiltaksgjennomforingAlert={tiltaksgjennomforing.faneinnhold?.detaljerOgInnholdInfoboks}
        tiltakstype={tiltakstypeSanityData?.faneinnhold?.detaljerOgInnhold}
        tiltakstypeAlert={tiltakstypeSanityData?.faneinnhold?.detaljerOgInnholdInfoboks}
      />
      <Heading size="medium">Påmelding og varighet</Heading>
      <DetaljerFane
        tiltaksgjennomforing={tiltaksgjennomforing.faneinnhold?.pameldingOgVarighet}
        tiltaksgjennomforingAlert={tiltaksgjennomforing.faneinnhold?.pameldingOgVarighetInfoboks}
        tiltakstype={tiltakstypeSanityData?.faneinnhold?.pameldingOgVarighet}
        tiltakstypeAlert={tiltakstypeSanityData?.faneinnhold?.pameldingOgVarighetInfoboks}
      />
    </div>
  );
};

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
