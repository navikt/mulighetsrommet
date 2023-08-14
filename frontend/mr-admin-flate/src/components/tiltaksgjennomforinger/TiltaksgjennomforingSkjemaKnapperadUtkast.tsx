import styles from "../skjema/Skjema.module.scss";
import {
  Tiltaksgjennomforing,
  TiltaksgjennomforingRequest,
  Toggles,
  Utkast,
} from "mulighetsrommet-api-client";
import { useState } from "react";
import { SlettUtkastKnapp } from "../knapper/SlettUtkastKnapp";
import SletteModal from "../modal/SletteModal";
import { OpprettAvtaleGjennomforingKnapp } from "../knapper/OpprettAvtaleGjennomforingKnapp";
import { UseMutationResult } from "@tanstack/react-query";
import { LagreEndringerKnapp } from "../knapper/LagreEndringerKnapp";
import { useFeatureToggle } from "../../api/features/feature-toggles";
import AvbrytAvtaleGjennomforingModal from "../avtaler/AvbrytAvtaleGjennomforingModal";

interface Props {
  onClose: () => void;
  tiltaksgjennomforing: Tiltaksgjennomforing;
  utkastModus: boolean;
  mutation: UseMutationResult<
    Tiltaksgjennomforing,
    unknown,
    TiltaksgjennomforingRequest
  >;
  onLagreUtkast: () => void;
  mutationUtkast: UseMutationResult<Utkast, unknown, Utkast>;
}

export function TiltaksgjennomforingSkjemaKnapperadUtkast({
  onClose,
  tiltaksgjennomforing,
  utkastModus,
  mutation,
  onLagreUtkast,
  mutationUtkast,
}: Props) {
  const { data: slettTiltaksgjennomforingEnabled } = useFeatureToggle(
    Toggles.MULIGHETSROMMET_ADMIN_FLATE_SLETT_TILTAKSGJENNOMFORING,
  );
  const [avbrytModalOpen, setAvbrytModalOpen] = useState(false);
  const [sletteModalOpen, setSletteModalOpen] = useState(false);

  return utkastModus ? (
    <>
      <div className={styles.button_row}>
        {slettTiltaksgjennomforingEnabled ? (
          <SlettUtkastKnapp setSletteModal={setSletteModalOpen} />
        ) : null}
        <div>
          <LagreEndringerKnapp
            submit={false}
            onLagreUtkast={onLagreUtkast}
            mutationUtkast={mutationUtkast}
          />
          <OpprettAvtaleGjennomforingKnapp
            type="gjennomføring"
            mutation={mutation}
          />
        </div>
      </div>

      <AvbrytAvtaleGjennomforingModal
        modalOpen={avbrytModalOpen}
        onClose={() => {
          setAvbrytModalOpen(false);
        }}
        mutation={mutationUtkast}
        data={tiltaksgjennomforing}
        type="gjennomforing"
      />
      <SletteModal
        modalOpen={sletteModalOpen}
        onClose={() => setSletteModalOpen(false)}
        headerText="Ønsker du å slette utkastet?"
        headerTextError="Kan ikke slette utkastet."
        handleDelete={onClose}
      />
    </>
  ) : null;
}
