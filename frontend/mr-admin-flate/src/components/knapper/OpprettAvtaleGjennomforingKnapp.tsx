import styles from "../skjema/Skjema.module.scss";
import { Button } from "@navikt/ds-react";
import { faro } from "@grafana/faro-web-sdk";

interface Props {
  type: "avtale" | "gjennomføring";
}
export function OpprettAvtaleGjennomforingKnapp({ type }: Props) {
  return (
    <Button
      className={styles.button}
      type="submit"
      onClick={() => {
        faro?.api?.pushEvent(
          "Bruker oppretter avtale",
          { handling: "oppretter" },
          type,
        );
      }}
    >
      Opprett {type}
    </Button>
  );
}
