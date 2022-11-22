import { Tiltaksgjennomforingeroversikt } from "../components/Tiltaksgjennomforingeroversikt";
import { BodyLong, Heading } from "@navikt/ds-react";
import styles from "./Oversikt.module.scss";

export function Oversikt() {
  return (
    <div>
      <Heading className={styles.overskrift} size={"medium"}>
        Min oversikt
      </Heading>
      <BodyLong className={styles.body} size={"small"}>
        Her finner du dine aktive gjennomføringer.
      </BodyLong>
      <Tiltaksgjennomforingeroversikt />
    </div>
  );
}
