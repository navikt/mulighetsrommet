import styles from "../skjema/Skjema.module.scss";
import { useFeatureToggle } from "../../api/features/feature-toggles";
import { Avtale, Avtalestatus, Toggles } from "mulighetsrommet-api-client";
import AvbrytAvtaleModal from "./AvbrytAvtaleModal";
import { useState } from "react";
import { LagreEndringerKnapp } from "../knapper/LagreEndringerKnapp";
import { AvbrytAvtaleGjennomforingKnapp } from "../knapper/AvbrytAvtaleGjennomforingKnapp";
import { ForkastEndringerKnapp } from "../knapper/ForkastEndringerKnapp";

interface Props {
  onClose: () => void;
  avtale: Avtale;
}
export function AvtaleSkjemaKnapperadRediger({ onClose, avtale }: Props) {
  const { data: slettAvtaleEnabled } = useFeatureToggle(
    Toggles.MULIGHETSROMMET_ADMIN_FLATE_SLETT_AVTALE,
  );
  const [avbrytModalOpen, setAvbrytModalOpen] = useState(false);

  return (
    <div className={styles.button_row}>
      {slettAvtaleEnabled && avtale?.avtalestatus === Avtalestatus.AKTIV ? (
        <AvbrytAvtaleGjennomforingKnapp
          setAvbrytModalOpen={setAvbrytModalOpen}
          type="avtale"
          dataTestId="avbryt-avtale"
        />
      ) : null}
      <div>
        <ForkastEndringerKnapp
          type="avtale"
          dataTestId="avtaleskjema-avbrytknapp"
          onClose={onClose}
        />
        <LagreEndringerKnapp />
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
