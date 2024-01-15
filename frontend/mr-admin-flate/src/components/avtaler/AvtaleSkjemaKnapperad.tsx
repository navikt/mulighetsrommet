import { Button } from "@navikt/ds-react";
import styles from "../skjema/Skjema.module.scss";
import { ValideringsfeilOppsummering } from "../skjema/ValideringsfeilOppsummering";
import { HarSkrivetilgang } from "../authActions/HarSkrivetilgang";

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
      <HarSkrivetilgang ressurs="Avtale">
        <Button className={styles.button} size="small" type="submit">
          {redigeringsModus ? "Lagre redigert avtale" : "Opprett ny avtale"}
        </Button>
      </HarSkrivetilgang>
    </div>
  );
}
