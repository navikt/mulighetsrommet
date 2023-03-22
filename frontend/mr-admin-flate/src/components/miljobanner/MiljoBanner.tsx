import { Alert, BodyShort, Button, Heading } from "@navikt/ds-react";
import { useState } from "react";
import { useVisForMiljo } from "../../hooks/useVisForMiljo";
import styles from "./MiljoBanner.module.scss";

const WHITELIST_BANNER = [".ekstern.dev.nav.no"];

export function MiljoBanner() {
  const [vis, setVis] = useState(true);
  const visForMiljo = useVisForMiljo(WHITELIST_BANNER);

  if (!visForMiljo) {
    return null;
  }

  if (!vis) return null;

  return (
    <div className={styles.miljobanner_container}>
      <Alert variant="warning">
        <Heading spacing size="small">
          Dette er en demo-tjeneste som er under utvikling
        </Heading>
        <BodyShort>
          Her eksperimenterer vi med ny funksjonalitet. Demoen inneholder ikke
          ekte data og kan til tider være ustabil.
        </BodyShort>
        <Button onClick={() => setVis(false)}>Lukk melding</Button>
      </Alert>
    </div>
  );
}
