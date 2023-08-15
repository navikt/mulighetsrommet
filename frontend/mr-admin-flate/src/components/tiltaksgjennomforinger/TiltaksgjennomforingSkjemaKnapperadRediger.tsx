import styles from "../skjema/Skjema.module.scss";
import {
  Tiltaksgjennomforing,
  TiltaksgjennomforingStatus,
  Toggles,
} from "mulighetsrommet-api-client";

import { useState } from "react";
import { LagreEndringerKnapp } from "../knapper/LagreEndringerKnapp";
import { AvbrytAvtaleGjennomforingKnapp } from "../knapper/AvbrytAvtaleGjennomforingKnapp";
import { arenaOpphav } from "./TiltaksgjennomforingSkjemaConst";
import { useFeatureToggle } from "../../api/features/feature-toggles";
import AvbrytAvtaleGjennomforingModal from "../avtaler/AvbrytAvtaleGjennomforingModal";
import { useAvbrytTiltaksgjennomforing } from "../../api/tiltaksgjennomforing/useAvbrytTiltaksgjennomforing";

interface Props {
  tiltaksgjennomforing: Tiltaksgjennomforing;
}

export function TiltaksgjennomforingSkjemaKnapperadRediger({
  tiltaksgjennomforing,
}: Props) {
  const { data: slettTiltaksgjennomforingEnabled } = useFeatureToggle(
    Toggles.MULIGHETSROMMET_ADMIN_FLATE_SLETT_TILTAKSGJENNOMFORING,
  );
  const [avbrytModalOpen, setAvbrytModalOpen] = useState(false);
  const mutationAvbryt = useAvbrytTiltaksgjennomforing();

  return (
    <div className={styles.button_row}>
      {slettTiltaksgjennomforingEnabled &&
      tiltaksgjennomforing?.status ===
        TiltaksgjennomforingStatus.GJENNOMFORES &&
      !arenaOpphav(tiltaksgjennomforing) ? (
        <AvbrytAvtaleGjennomforingKnapp
          onClick={() => setAvbrytModalOpen(true)}
          type="gjennomføring"
          dataTestId="avbryt-gjennomforing"
        />
      ) : null}
      <LagreEndringerKnapp onLagreUtkast={() => null} submit />

      <AvbrytAvtaleGjennomforingModal
        modalOpen={avbrytModalOpen}
        onClose={() => {
          setAvbrytModalOpen(false);
        }}
        mutationAvbryt={mutationAvbryt}
        data={tiltaksgjennomforing}
        type="gjennomforing"
      />
    </div>
  );
}
