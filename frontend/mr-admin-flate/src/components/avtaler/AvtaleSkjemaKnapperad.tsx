import styles from "./AvtaleSkjema.module.scss";
import { Button } from "@navikt/ds-react";
import { faro } from "@grafana/faro-web-sdk";

interface Props {
  redigeringsModus: boolean;
  onClose: () => void;
}
export function AvtaleSkjemaKnapperad({ redigeringsModus, onClose }: Props) {
  return (
    <div className={styles.button_row}>
      <Button
        className={styles.button}
        onClick={onClose}
        variant="secondary"
        data-testid="avtaleskjema-avbrytknapp"
      >
        Avbryt
      </Button>
      <Button
        className={styles.button}
        type="submit"
        onClick={() => {
          faro?.api?.pushEvent(
            `Bruker ${redigeringsModus ? "redigerer" : "oppretter"} avtale`,
            { handling: redigeringsModus ? "redigerer" : "oppretter" },
            "avtale",
          );
        }}
      >
        {redigeringsModus ? "Lagre redigert avtale" : "Opprett avtale"}
      </Button>
    </div>
  );
}
