import styles from "../skjema/Skjema.module.scss";
import {
  Tiltaksgjennomforing,
  TiltaksgjennomforingRequest,
  TiltaksgjennomforingStatus,
  Toggles,
  Utkast,
} from "mulighetsrommet-api-client";

import { useState } from "react";
import { LagreEndringerKnapp } from "../knapper/LagreEndringerKnapp";
import { AvbrytAvtaleGjennomforingKnapp } from "../knapper/AvbrytAvtaleGjennomforingKnapp";
import { UseMutationResult } from "@tanstack/react-query";
import { arenaOpphav } from "./TiltaksgjennomforingSkjemaConst";
import { useFeatureToggle } from "../../api/features/feature-toggles";
import AvbrytAvtaleGjennomforingModal from "../avtaler/AvbrytAvtaleGjennomforingModal";

interface Props {
  tiltaksgjennomforing: Tiltaksgjennomforing;
  mutationUtkast: UseMutationResult<Utkast, unknown, Utkast>;
  mutation: UseMutationResult<
    Tiltaksgjennomforing,
    unknown,
    TiltaksgjennomforingRequest
  >;
}

export function TiltaksgjennomforingSkjemaKnapperadRediger({
  tiltaksgjennomforing,
  mutationUtkast,
  mutation,
}: Props) {
  const { data: slettTiltaksgjennomforingEnabled } = useFeatureToggle(
    Toggles.MULIGHETSROMMET_ADMIN_FLATE_SLETT_TILTAKSGJENNOMFORING,
  );
  const [avbrytModalOpen, setAvbrytModalOpen] = useState(false);

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
      <LagreEndringerKnapp
        onLagreUtkast={() => null}
        mutationUtkast={mutationUtkast}
      />

      <AvbrytAvtaleGjennomforingModal
        modalOpen={avbrytModalOpen}
        onClose={() => {
          setAvbrytModalOpen(false);
        }}
        mutation={mutation}
        data={tiltaksgjennomforing}
        type="gjennomforing"
      />
    </div>
  );
}
