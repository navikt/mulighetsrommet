import styles from "../skjema/Skjema.module.scss";
import { Button } from "@navikt/ds-react";
import { faro } from "@grafana/faro-web-sdk";

interface Props {
  onClose: () => void;
  type: "avtale" | "gjennomføring";
  dataTestId?: string;
}
export function ForkastEndringerKnapp({ onClose, type, dataTestId }: Props) {
  return (
    <Button
      className={styles.button}
      onClick={() => {
        onClose();
        faro?.api?.pushEvent(
          "Bruker forkaster endringer",
          { handling: "forkaster" },
          type,
        );
      }}
      variant="tertiary"
      data-testid={dataTestId}
      type="button"
    >
      Forkast endringer
    </Button>
  );
}
