import styles from "../skjema/Skjema.module.scss";
import { Button } from "@navikt/ds-react";
import { faro } from "@grafana/faro-web-sdk";

export function LagreEndringerKnapp() {
  return (
    <Button
      className={styles.button}
      type="submit"
      onClick={() => {
        faro?.api?.pushEvent(
          "Bruker redigerer avtale",
          { handling: "redigerer" },
          "avtale",
        );
      }}
    >
      Lagre endringer
    </Button>
  );
}
