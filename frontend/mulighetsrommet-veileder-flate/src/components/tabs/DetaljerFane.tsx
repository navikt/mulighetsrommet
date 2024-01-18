import { Alert, BodyLong, Heading } from "@navikt/ds-react";
import styles from "./Detaljerfane.module.scss";
import FaneTiltaksinformasjon from "./FaneTiltaksinformasjon";
import { RedaksjoneltInnhold } from "../RedaksjoneltInnhold";

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
  return (
    <FaneTiltaksinformasjon
      className={styles.faneinnhold_container}
      harInnhold={
        tiltaksgjennomforingAlert || tiltakstypeAlert || tiltaksgjennomforing || tiltakstype
      }
    >
      <Heading level="2" size="small">
        Generell informasjon
      </Heading>
      {tiltakstypeAlert && (
        <Alert variant="info" className={styles.preWrap}>
          {tiltakstypeAlert}
        </Alert>
      )}
      <BodyLong as="div" size="small">
        <RedaksjoneltInnhold value={tiltakstype} />
      </BodyLong>
      {(tiltaksgjennomforing || tiltaksgjennomforingAlert) && (
        <div className={styles.lokal_informasjon}>
          <Heading level="2" size="small">
            Lokal informasjon
          </Heading>
          {tiltaksgjennomforingAlert && (
            <Alert variant="info" className={styles.preWrap}>
              {tiltaksgjennomforingAlert}
            </Alert>
          )}
          <BodyLong as="div" textColor="subtle" size="small">
            <RedaksjoneltInnhold value={tiltaksgjennomforing} />
          </BodyLong>
        </div>
      )}
    </FaneTiltaksinformasjon>
  );
};

export default DetaljerFane;
