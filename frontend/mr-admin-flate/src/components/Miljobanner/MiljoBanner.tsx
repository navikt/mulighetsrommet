import { Alert, Heading, Button } from "@navikt/ds-react";
import { useState } from "react";
import styles from "./MiljoBanner.module.scss";

const WHITELIST_BANNER = ["labs.nais.io"] as const;

export function MiljoBanner() {
  const [vis, setVis] = useState(true);
  const url = window?.location?.host as typeof WHITELIST_BANNER[number];

  if (
    !WHITELIST_BANNER.find((el) => url.toLowerCase().includes(el.toLowerCase()))
  ) {
    return null;
  }

  const navnForMiljo = (url: typeof WHITELIST_BANNER[number]) => {
    switch (url) {
      case "labs.nais.io":
        return "labs";
    }
  };

  if (!vis) return null;

  return (
    <div className={styles.miljobanner_container}>
      <Alert variant="warning">
        <Heading spacing size="small">
          Dette er en demo-tjeneste i <code>{navnForMiljo(url)}</code>
          -miljøet som er under utvikling
        </Heading>
        <p>
          Her eksperimenterer vi med ny funksjonalitet. Demoen inneholder ikke
          ekte data og kan til tider være ustabil.
        </p>
        <Button onClick={() => setVis(false)}>Lukk melding</Button>
      </Alert>
    </div>
  );
}
