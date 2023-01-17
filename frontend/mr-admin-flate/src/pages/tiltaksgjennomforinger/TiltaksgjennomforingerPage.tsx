import { Tiltaksgjennomforingeroversikt } from "../../components/tiltaksgjennomforinger/Tiltaksgjennomforingeroversikt";
import { Alert, BodyShort, Heading } from "@navikt/ds-react";
import styles from "../Oversikt.module.scss";
import { SokEtterTiltaksgjennomforing } from "../../components/sok/SokEtterTiltaksgjennomforing";
import { Laster } from "../../components/Laster";
import { useTiltaksgjennomforingerByInnloggetAnsatt } from "../../api/tiltaksgjennomforing/useTiltaksgjennomforingerByInnloggetAnsatt";

export function TiltaksgjennomforingerPage() {
  const { data, isFetching, isError } =
    useTiltaksgjennomforingerByInnloggetAnsatt();
  if (isFetching) {
    return <Laster />;
  }

  if (isError) {
    return (
      <Alert variant="error">
        Det oppsto en feil ved henting av tiltaksgjennomføringer. Prøv igjen
        senere.
      </Alert>
    );
  }

  if (!data) {
    return (
      <Alert variant="warning">Klarte ikke finne tiltaksgjennomføringer.</Alert>
    );
  }

  return (
    <>
      <Heading size="large">Oversikt over tiltaksgjennomføringer</Heading>
      <BodyShort className={styles.body} size="small">
        Her finner du alle gjennomføringer
      </BodyShort>
      <SokEtterTiltaksgjennomforing />
      <Tiltaksgjennomforingeroversikt />
    </>
  );
}
