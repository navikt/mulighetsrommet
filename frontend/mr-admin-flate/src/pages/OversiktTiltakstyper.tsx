import { BodyLong, Heading } from "@navikt/ds-react";
import styles from "./Oversikt.module.scss";
import { Link } from "react-router-dom";
import { Tiltakstyperoversikt } from "../components/tiltakstyper/Tiltakstyperoversikt";

export function OversiktTiltakstyper() {
  return (
    <div>
      <Link to="/">Hjem</Link>
      <Heading className={styles.overskrift} size={"medium"}>
        Oversikt over tiltakstyper
      </Heading>
      <BodyLong className={styles.body} size={"small"}>
        Her finner du dine aktive tiltakstyper.
      </BodyLong>
      <Tiltakstyperoversikt />
    </div>
  );
}
