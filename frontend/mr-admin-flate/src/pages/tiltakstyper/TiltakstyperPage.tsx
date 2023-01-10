import { Heading } from "@navikt/ds-react";
import { Link } from "react-router-dom";
import { useFeatureToggles } from "../../api/features/feature-toggles";
import { TiltakstyperOversikt } from "../../components/tiltakstyper/TiltakstyperOversikt";
import styles from "../tiltaksgjennomforinger/Oversikt.module.scss";

export function TiltakstyperPage() {
  const { data: toggles } = useFeatureToggles();

  return (
    <>
      <Heading size="large">Oversikt over tiltakstyper</Heading>
      {toggles?.["mulighetsrommet.enable-opprett-tiltakstype"] ? (
        <Link to="tiltakstyper/opprett">Opprett tiltakstype</Link>
      ) : null}

      <BodyShort className={styles.body} size="small">
        Her finner du dine aktive tiltakstyper.
      </BodyShort>
      <TiltakstyperOversikt />
    </>
  );
}
