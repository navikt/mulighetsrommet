import { Button } from "@navikt/ds-react";
import styles from "../skjema/Skjema.module.scss";
import { faro } from "@grafana/faro-web-sdk";

interface Props {
  onLagreUtkast: () => void;
  type: "avtale" | "gjennomføring";
  dataTestId?: string;
}
export function LagreUtkastKnapp({ onLagreUtkast, type, dataTestId }: Props) {
  return (
    <Button
      className={styles.button}
      type="button"
      onClick={() => {
        onLagreUtkast();
        faro?.api?.pushEvent(
          "Bruker lagrer utkast",
          { handling: "lagrer" },
          type,
        );
      }}
      variant="secondary"
      data-testid={dataTestId}
    >
      Lagre som utkast
    </Button>
  );
}
