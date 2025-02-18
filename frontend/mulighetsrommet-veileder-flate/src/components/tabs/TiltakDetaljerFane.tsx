import { PortableTextTypedObject } from "@mr/api-client-v2";
import { LokalInformasjonContainer } from "@mr/frontend-common";
import { Alert, BodyLong, Heading } from "@navikt/ds-react";
import { RedaksjoneltInnhold } from "../RedaksjoneltInnhold";
import styles from "./TiltakDetaljerFane.module.scss";
import { TiltakDetaljerFaneContainer } from "./TiltakDetaljerFaneContainer";

interface DetaljerFaneProps {
  gjennomforingAlert?: string;
  tiltakstypeAlert?: string;
  gjennomforing?: PortableTextTypedObject[];
  tiltakstype?: PortableTextTypedObject[];
}

export function TiltakDetaljerFane({
  gjennomforingAlert,
  tiltakstypeAlert,
  gjennomforing,
  tiltakstype,
}: DetaljerFaneProps) {
  return (
    <TiltakDetaljerFaneContainer
      className={styles.faneinnhold_container}
      harInnhold={!!gjennomforingAlert || !!tiltakstypeAlert || !!gjennomforing || !!tiltakstype}
    >
      {tiltakstypeAlert && (
        <Alert variant="info" style={{ whiteSpace: "pre-wrap" }}>
          {tiltakstypeAlert}
        </Alert>
      )}
      <BodyLong as="div" size="large" className="prose">
        <RedaksjoneltInnhold value={tiltakstype} />
      </BodyLong>
      {(gjennomforing || gjennomforingAlert) && (
        <LokalInformasjonContainer>
          <Heading level="2" size="small">
            Lokal informasjon
          </Heading>
          {gjennomforingAlert && (
            <Alert variant="info" style={{ whiteSpace: "pre-wrap", margin: "1rem 0" }}>
              {gjennomforingAlert}
            </Alert>
          )}
          <RedaksjoneltInnhold value={gjennomforing} />
        </LokalInformasjonContainer>
      )}
    </TiltakDetaljerFaneContainer>
  );
}
