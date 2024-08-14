import { Alert, BodyLong, Heading } from "@navikt/ds-react";
import styles from "./Detaljerfane.module.scss";
import FaneTiltaksinformasjon from "./FaneTiltaksinformasjon";
import { RedaksjoneltInnhold } from "../RedaksjoneltInnhold";
import { LokalInformasjonContainer } from "@mr/frontend-common";

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
        <Alert variant="info" style={{ whiteSpace: "pre-wrap" }}>
          {tiltakstypeAlert}
        </Alert>
      )}
      <BodyLong as="div" size="small">
        <RedaksjoneltInnhold value={tiltakstype} />
      </BodyLong>
      {(tiltaksgjennomforing || tiltaksgjennomforingAlert) && (
        <LokalInformasjonContainer>
          <Heading level="2" size="small">
            Lokal informasjon
          </Heading>
          {tiltaksgjennomforingAlert && (
            <Alert variant="info" style={{ whiteSpace: "pre-wrap" }}>
              {tiltaksgjennomforingAlert}
            </Alert>
          )}
          <BodyLong as="div" textColor="subtle" size="small">
            <RedaksjoneltInnhold value={tiltaksgjennomforing} />
          </BodyLong>
        </LokalInformasjonContainer>
      )}
    </FaneTiltaksinformasjon>
  );
};

export default DetaljerFane;
