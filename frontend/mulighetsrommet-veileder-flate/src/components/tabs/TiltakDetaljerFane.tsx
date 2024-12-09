import { PortableTextTypedObject } from "@mr/api-client";
import { LokalInformasjonContainer } from "@mr/frontend-common";
import { Alert, BodyLong, Heading } from "@navikt/ds-react";
import { RedaksjoneltInnhold } from "../RedaksjoneltInnhold";
import styles from "./TiltakDetaljerFane.module.scss";
import { TiltakDetaljerFaneContainer } from "./TiltakDetaljerFaneContainer";

interface DetaljerFaneProps {
  tiltaksgjennomforingAlert?: string;
  tiltakstypeAlert?: string;
  tiltaksgjennomforing?: PortableTextTypedObject[];
  tiltakstype?: PortableTextTypedObject[];
}

export function TiltakDetaljerFane({
  tiltaksgjennomforingAlert,
  tiltakstypeAlert,
  tiltaksgjennomforing,
  tiltakstype,
}: DetaljerFaneProps) {
  return (
    <TiltakDetaljerFaneContainer
      className={styles.faneinnhold_container}
      harInnhold={
        !!tiltaksgjennomforingAlert || !!tiltakstypeAlert || !!tiltaksgjennomforing || !!tiltakstype
      }
    >
      {tiltakstypeAlert && (
        <Alert variant="info" style={{ whiteSpace: "pre-wrap" }}>
          {tiltakstypeAlert}
        </Alert>
      )}
      <BodyLong as="div" size="large">
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
          <BodyLong as="div" size="large">
            <RedaksjoneltInnhold value={tiltaksgjennomforing} />
          </BodyLong>
        </LokalInformasjonContainer>
      )}
    </TiltakDetaljerFaneContainer>
  );
}
