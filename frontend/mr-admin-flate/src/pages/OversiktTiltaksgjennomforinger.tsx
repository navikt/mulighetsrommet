import { Tiltaksgjennomforingeroversikt } from "../components/tiltaksgjennomforinger/Tiltaksgjennomforingeroversikt";
import { BodyLong, Heading } from "@navikt/ds-react";
import styles from "./Oversikt.module.scss";
import Tilbakeknapp from "mulighetsrommet-veileder-flate/src/components/tilbakeknapp/Tilbakeknapp";

export function OversiktTiltaksgjennomforinger() {
  return (
    <>
      <Tilbakeknapp tilbakelenke="/" tekst="Hjem" />
      <Heading className={styles.overskrift} size="large">
        Oversikt over tiltaksgjennomføringer
      </Heading>
      <BodyLong className={styles.body} size="small">
        Her finner du dine aktive gjennomføringer.
      </BodyLong>
      <Tiltaksgjennomforingeroversikt />
    </>
  );
}
