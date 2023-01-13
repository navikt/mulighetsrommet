import { Tiltaksgjennomforingeroversikt } from "../../components/tiltaksgjennomforinger/Tiltaksgjennomforingeroversikt";
import { BodyShort, Heading } from "@navikt/ds-react";
import styles from "./Oversikt.module.scss";
import { SokEtterTiltaksgjennomforing } from "../../components/sok/SokEtterTiltaksgjennomforing";

export function TiltaksgjennomforingerPage() {
  return (
    <>
      <Heading className={styles.overskrift} size="large">
        Oversikt over tiltaksgjennomføringer
      </Heading>
      <BodyShort className={styles.body} size="small">
        Her finner du alle gjennomføringer
      </BodyShort>
      <SokEtterTiltaksgjennomforing />
      <Tiltaksgjennomforingeroversikt />
    </>
  );
}
