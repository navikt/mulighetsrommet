import { Alert, BodyShort, Button, Heading } from "@navikt/ds-react";
import { useState } from "react";
import { useVisForMiljo } from "../../hooks/useVisForMiljo";
import styles from "./MiljoBanner.module.scss";

const WHITELIST_BANNER = ["labs.nais.io"];

export function MiljoBanner() {
  const [vis, setVis] = useState(true);
  const visForMiljo = useVisForMiljo(WHITELIST_BANNER);
  const url = window?.location?.host;

  if (!visForMiljo) {
    return null;
  }

  const navnForMiljo = (url: string) => {
    if (url.includes("labs.nais.io")) {
      return "labs";
    }
    return "";
  };

  if (!vis) return null;

  return (
    <div className={styles.miljobanner_container}>
      <Alert variant="warning">
        <Heading spacing size="small">
          Dette er en demo-tjeneste i <code>{navnForMiljo(url)}</code> som er
          under utvikling
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
