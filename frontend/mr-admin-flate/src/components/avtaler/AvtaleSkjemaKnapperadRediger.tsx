import styles from "../skjema/Skjema.module.scss";
import { Button } from "@navikt/ds-react";
import { faro } from "@grafana/faro-web-sdk";

interface Props {
  onClose: () => void;
}
export function AvtaleSkjemaKnapperadRediger({ onClose }: Props) {
  return (
    <div className={styles.button_row}>
      <div>
        <Button
          className={styles.button}
          onClick={() => {
            onClose();
            faro?.api?.pushEvent(
              "Bruker avbryter avtale",
              { handling: "avbryter" },
              "avtale",
            );
          }}
          variant="tertiary"
          data-testid="avtaleskjema-avbrytknapp"
        >
          Avbryt
        </Button>

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
          Lagre redigert avtale
        </Button>
      </div>
    </div>
  );
}
