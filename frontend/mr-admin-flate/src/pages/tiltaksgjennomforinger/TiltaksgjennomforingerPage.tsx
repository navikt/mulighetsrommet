import { Tiltaksgjennomforingeroversikt } from "../../components/tiltaksgjennomforinger/Tiltaksgjennomforingeroversikt";
import { BodyLong, Button, Heading } from "@navikt/ds-react";
import styles from "./Oversikt.module.scss";
import { Link } from "react-router-dom";
import { SokEtterTiltaksgjennomforing } from "../../components/sok/SokEtterTiltaksgjennomforing";
import React from "react";

export function TiltaksgjennomforingerPage() {
  return (
    <>
      <Link to="/">Hjem</Link>
      <Heading className={styles.overskrift} size="large">
        Oversikt over tiltaksgjennomføringer
      </Heading>
      <div className={styles.opprettknappseksjon}>
        {
          //toggles?.["mulighetsrommet.enable-opprett-tiltakstype"] ? (
          <Link style={{ textDecoration: "none" }} to="/opprett">
            <Button variant="tertiary">Opprett ny tiltaksgjennomføring</Button>
          </Link>
          //) : null // TODO legg til toggle
        }
      </div>
      <BodyLong className={styles.body} size="small">
        Her finner du alle gjennomføringer
      </BodyLong>
      <SokEtterTiltaksgjennomforing />
      <Tiltaksgjennomforingeroversikt />
    </>
  );
}
