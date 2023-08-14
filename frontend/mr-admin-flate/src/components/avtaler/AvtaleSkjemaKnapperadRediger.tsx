import styles from "../skjema/Skjema.module.scss";
import {
  Avtale,
  Avtalestatus,
  Toggles,
  Utkast,
} from "mulighetsrommet-api-client";
import AvbrytAvtaleGjennomforingModal from "./AvbrytAvtaleGjennomforingModal";
import { useState } from "react";
import { LagreEndringerKnapp } from "../knapper/LagreEndringerKnapp";
import { AvbrytAvtaleGjennomforingKnapp } from "../knapper/AvbrytAvtaleGjennomforingKnapp";
import { UseMutationResult } from "@tanstack/react-query";
import { useFeatureToggle } from "../../api/features/feature-toggles";
import { useAvbrytAvtale } from "../../api/avtaler/useAvbrytAvtale";

interface Props {
  avtale: Avtale;
  mutationUtkast: UseMutationResult<Utkast, unknown, Utkast>;
}

export function AvtaleSkjemaKnapperadRediger({
  avtale,
  mutationUtkast,
}: Props) {
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
        <LagreEndringerKnapp
          onLagreUtkast={() => null}
          mutationUtkast={mutationUtkast}
          submit
        />
      </div>
      <AvbrytAvtaleGjennomforingModal
        modalOpen={avbrytModalOpen}
        onClose={() => {
          setAvbrytModalOpen(false);
        }}
        data={avtale}
        mutation={mutationAvbryt}
        type="avtale"
      />
    </div>
  );
}
