import { Tiltaksgjennomforingeroversikt } from "../components/tiltaksgjennomforinger/Tiltaksgjennomforingeroversikt";
import { BodyLong, Heading } from "@navikt/ds-react";
import styles from "./Oversikt.module.scss";
import { Link } from "react-router-dom";

export function Oversikt() {
  return (
    <div>
      <Link to="/">Hjem</Link>
      <Heading className={styles.overskrift} size={"medium"}>
        Oversikt over tiltaksgjennomføringer
      </Heading>
      <BodyLong className={styles.body} size={"small"}>
        Her finner du dine aktive gjennomføringer.
      </BodyLong>
      <Tiltaksgjennomforingeroversikt />
    </div>
  );
}
