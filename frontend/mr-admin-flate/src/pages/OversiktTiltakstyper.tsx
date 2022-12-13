import { BodyLong, Heading } from "@navikt/ds-react";
import styles from "./Oversikt.module.scss";
import { TiltakstyperOversikt } from "../components/tiltakstyper/TiltakstyperOversikt";
import Tilbakeknapp from "mulighetsrommet-veileder-flate/src/components/tilbakeknapp/Tilbakeknapp";

export function OversiktTiltakstyper() {
  return (
    <>
      <Tilbakeknapp tilbakelenke="/" tekst="Hjem" />
      <Heading className={styles.overskrift} size="large">
        Oversikt over tiltakstyper
      </Heading>
      <BodyLong className={styles.body} size="small">
        Her finner du dine aktive tiltakstyper.
      </BodyLong>
      <TiltakstyperOversikt />
    </>
  );
}
