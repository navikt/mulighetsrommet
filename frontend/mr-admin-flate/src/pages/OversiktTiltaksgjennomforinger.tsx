import { Tiltaksgjennomforingeroversikt } from "../components/tiltaksgjennomforinger/Tiltaksgjennomforingeroversikt";
import { BodyLong, Heading } from "@navikt/ds-react";
import styles from "./Oversikt.module.scss";
import { Link } from "react-router-dom";
import { SokEtterTiltaksgjennomforing } from "../components/sok/SokEtterTiltaksgjennomforing";

export function OversiktTiltaksgjennomforinger() {
  return (
    <>
      <Link to="/">Hjem</Link>
      <Heading className={styles.overskrift} size="large">
        Oversikt over tiltaksgjennomføringer
      </Heading>
      <BodyLong className={styles.body} size="small">
        Her finner du alle gjennomføringer
      </BodyLong>
      <SokEtterTiltaksgjennomforing />
      <Tiltaksgjennomforingeroversikt />
    </>
  );
}
