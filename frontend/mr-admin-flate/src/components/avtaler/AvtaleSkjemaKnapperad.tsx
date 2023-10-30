import styles from "../skjema/Skjema.module.scss";
import { Button } from "@navikt/ds-react";
import { faro } from "@grafana/faro-web-sdk";
import { AutoSaveUtkast } from "../autosave/AutoSaveUtkast";
import { ValideringsfeilOppsummering } from "../skjema/ValideringsfeilOppsummering";

interface Props {
  redigeringsModus: boolean;
  onClose: () => void;
  defaultValues: any;
  utkastIdRef: string;
  saveUtkast: () => void;
  mutationUtkast: any;
  lagreState?: string;
  setLagreState: (state: string) => void;
}
export function AvtaleSkjemaKnapperad({
  redigeringsModus,
  onClose,
  defaultValues,
  utkastIdRef,
  saveUtkast,
  mutationUtkast,
  lagreState,
  setLagreState,
}: Props) {
  return (
    <div className={styles.knapperad}>
      <AutoSaveUtkast
        defaultValues={defaultValues}
        utkastId={utkastIdRef}
        onSave={() => saveUtkast()}
        mutationUtkast={mutationUtkast}
        lagreState={lagreState}
        setLagreState={setLagreState}
      />
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
