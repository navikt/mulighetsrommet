import { TiltaksgjennomforingerOversikt } from "../../components/tiltaksgjennomforinger/TiltaksgjennomforingerOversikt";
import { Link } from "react-router-dom";
import React from "react";
import { Alert, BodyShort, Heading, Button } from "@navikt/ds-react";
import styles from "../Oversikt.module.scss";
import pageStyles from "../Page.module.scss";
import { SokEtterTiltaksgjennomforing } from "../../components/sok/SokEtterTiltaksgjennomforing";
import { Laster } from "../../components/Laster";
import { useTiltaksgjennomforingerByInnloggetAnsatt } from "../../api/tiltaksgjennomforing/useTiltaksgjennomforingerByInnloggetAnsatt";
import { useFeatureToggles } from "../../api/features/feature-toggles";

export function TiltaksgjennomforingerPage() {
  const { data: toggles } = useFeatureToggles();
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
      <div className={pageStyles.header_wrapper}>
        <Heading size="large">Oversikt over tiltaksgjennomføringer</Heading>
        {toggles?.["mulighetsrommet.enable-opprett-gjennomforing"] ? (
          <Link to="/opprett-tiltaksgjennomforing" className={styles.opprettknappseksjon}>
            <Button variant="tertiary">Opprett ny tiltaksgjennomføring</Button>
          </Link>
        ) : null}
      </div>
      <BodyShort className={styles.body} size="small">
        Her finner du alle gjennomføringer
      </BodyShort>
      <SokEtterTiltaksgjennomforing />
      <TiltaksgjennomforingerOversikt />
    </>
  );
}
