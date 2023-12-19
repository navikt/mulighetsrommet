import styles from "../skjema/Skjema.module.scss";
import { Button } from "@navikt/ds-react";
import { faro } from "@grafana/faro-web-sdk";
import { ValideringsfeilOppsummering } from "../skjema/ValideringsfeilOppsummering";

interface Props {
  redigeringsModus: boolean;
  onClose: () => void;
}
export function AvtaleSkjemaKnapperad({ redigeringsModus, onClose }: Props) {
  return (
    <div className={styles.knapperad}>
      <ValideringsfeilOppsummering />
      <Button
        size="small"
        className={styles.button}
        onClick={onClose}
        variant="tertiary"
        type="button"
      >
        Avbryt
      </Button>
      <Button
        className={styles.button}
        size="small"
        type="submit"
        onClick={() => {
          faro?.api?.pushEvent(
            `Bruker ${redigeringsModus ? "redigerer" : "oppretter"} avtale`,
            {
              handling: redigeringsModus ? "redigerer" : "oppretter",
            },
            "avtale",
          );
        }}
      >
        {redigeringsModus ? "Lagre redigert avtale" : "Opprett ny avtale"}
      </Button>
    </div>
  );
}
