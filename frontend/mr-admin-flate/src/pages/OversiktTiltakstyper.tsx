import { BodyLong, Heading } from "@navikt/ds-react";
import styles from "./Oversikt.module.scss";
import { TiltakstyperOversikt } from "../components/tiltakstyper/TiltakstyperOversikt";
import { Link } from "react-router-dom";

export function OversiktTiltakstyper() {
  return (
    <>
      <Link to="/">Hjem</Link>
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
