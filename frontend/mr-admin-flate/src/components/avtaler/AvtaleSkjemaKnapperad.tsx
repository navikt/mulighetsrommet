import styles from "../skjema/Skjema.module.scss";
import { Button } from "@navikt/ds-react";
import { faro } from "@grafana/faro-web-sdk";
import { AutoSaveUtkast } from "../autosave/AutoSaveUtkast";

interface Props {
  redigeringsModus: boolean;
  onClose: () => void;
  defaultValues: any;
  utkastIdRef: string;
  defaultUpdatedAt?: string;
  saveUtkast: () => void;
  mutationUtkast: any;
}
export function AvtaleSkjemaKnapperad({
  redigeringsModus,
  onClose,
  defaultValues,
  utkastIdRef,
  defaultUpdatedAt,
  saveUtkast,
  mutationUtkast,
}: Props) {
  return (
    <div className={styles.knapperad}>
      <AutoSaveUtkast
        defaultValues={defaultValues}
        utkastId={utkastIdRef}
        defaultUpdatedAt={defaultUpdatedAt}
        onSave={() => saveUtkast()}
        mutationUtkast={mutationUtkast}
      />
      <Button
        size="small"
        className={styles.button}
        onClick={onClose}
        variant="tertiary"
        data-testid="avtaleskjema-avbrytknapp"
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
