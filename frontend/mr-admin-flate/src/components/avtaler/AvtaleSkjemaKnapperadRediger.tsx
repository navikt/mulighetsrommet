import styles from "../skjema/Skjema.module.scss";
import { Button } from "@navikt/ds-react";
import { faro } from "@grafana/faro-web-sdk";
import { useFeatureToggle } from "../../api/features/feature-toggles";
import { Avtale, Avtalestatus, Toggles } from "mulighetsrommet-api-client";
import AvbrytAvtaleModal from "./AvbrytAvtaleModal";
import { useState } from "react";

interface Props {
  onClose: () => void;
  avtale: Avtale;
}
export function AvtaleSkjemaKnapperadRediger({ onClose, avtale }: Props) {
  const { data: slettAvtaleEnabled } = useFeatureToggle(
    Toggles.MULIGHETSROMMET_ADMIN_FLATE_SLETT_AVTALE,
  );
  const utkastModus = !avtale?.avtalestatus;
  const [avbrytModalOpen, setAvbrytModalOpen] = useState(false);

  return utkastModus ? (
    <div className={styles.button_row}>
      {slettAvtaleEnabled ? (
        <Button variant="danger" type="button">
          Feilregistrering
        </Button>
      ) : null}
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
          type="button"
        >
          Forkast endringer
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
          Lagre endringer
        </Button>
      </div>
    </div>
  ) : (
    <div className={styles.button_row}>
      {slettAvtaleEnabled && avtale?.avtalestatus === Avtalestatus.AKTIV ? (
        <Button
          variant="danger"
          onClick={() => setAvbrytModalOpen(true)}
          data-testid="avbryt-avtale"
          type="button"
        >
          Avbryt avtale
        </Button>
      ) : null}
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
          type="button"
        >
          Forkast endringer
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
          Lagre endringer
        </Button>
      </div>
      <AvbrytAvtaleModal
        modalOpen={avbrytModalOpen}
        onClose={() => {
          setAvbrytModalOpen(false);
        }}
        avtale={avtale}
      />
    </div>
  );
}
