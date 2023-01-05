import { BodyLong, Heading } from "@navikt/ds-react";
import { Link } from "react-router-dom";
import { TiltakstyperOversikt } from "../../components/tiltakstyper/TiltakstyperOversikt";
import styles from "../tiltaksgjennomforinger/Oversikt.module.scss";

export function TiltakstyperPage() {
  return (
    <>
      <Link to="/">Hjem</Link>
      <Heading className={styles.overskrift} size="large">
        Oversikt over tiltakstyper
      </Heading>
      <Link to="opprett">Opprett tiltakstype</Link>
      <BodyLong className={styles.body} size="small">
        Her finner du dine aktive tiltakstyper.
      </BodyLong>
      <TiltakstyperOversikt />
    </>
  );
}
