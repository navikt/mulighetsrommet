import styles from "../skjema/Skjema.module.scss";
import { Avtale, Avtalestatus, Toggles } from "mulighetsrommet-api-client";
import AvbrytAvtaleGjennomforingModal from "./AvbrytAvtaleGjennomforingModal";
import { useState } from "react";
import { LagreEndringerKnapp } from "../knapper/LagreEndringerKnapp";
import { AvbrytAvtaleGjennomforingKnapp } from "../knapper/AvbrytAvtaleGjennomforingKnapp";
import { useFeatureToggle } from "../../api/features/feature-toggles";
import { useAvbrytAvtale } from "../../api/avtaler/useAvbrytAvtale";

interface Props {
  avtale: Avtale;
}

export function AvtaleSkjemaKnapperadRediger({ avtale }: Props) {
  const { data: slettAvtaleEnabled } = useFeatureToggle(
    Toggles.MULIGHETSROMMET_ADMIN_FLATE_SLETT_AVTALE,
  );
  const [avbrytModalOpen, setAvbrytModalOpen] = useState(false);
  const mutationAvbryt = useAvbrytAvtale();

  return (
    <div className={styles.button_row}>
      {slettAvtaleEnabled && avtale?.avtalestatus === Avtalestatus.AKTIV ? (
        <AvbrytAvtaleGjennomforingKnapp
          onClick={() => setAvbrytModalOpen(true)}
          type="avtale"
          dataTestId="avbryt-avtale"
        />
      ) : null}
      <div>
        <LagreEndringerKnapp onLagreUtkast={() => null} submit />
      </div>
      <AvbrytAvtaleGjennomforingModal
        modalOpen={avbrytModalOpen}
        onClose={() => {
          setAvbrytModalOpen(false);
        }}
        data={avtale}
        mutationAvbryt={mutationAvbryt}
        type="avtale"
      />
    </div>
  );
}
