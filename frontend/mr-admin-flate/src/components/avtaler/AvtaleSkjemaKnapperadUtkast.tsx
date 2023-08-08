import styles from "../skjema/Skjema.module.scss";
import { useFeatureToggle } from "../../api/features/feature-toggles";
import {
  Avtale,
  AvtaleRequest,
  Toggles,
  Utkast,
} from "mulighetsrommet-api-client";
import AvbrytAvtaleModal from "./AvbrytAvtaleModal";
import { useState } from "react";
import { SlettUtkastKnapp } from "../knapper/SlettUtkastKnapp";
import SletteModal from "../modal/SletteModal";
import { OpprettAvtaleGjennomforingKnapp } from "../knapper/OpprettAvtaleGjennomforingKnapp";
import { UseMutationResult } from "@tanstack/react-query";
import { LagreEndringerKnapp } from "../knapper/LagreEndringerKnapp";

interface Props {
  onClose: () => void;
  avtale: Avtale;
  utkastModus: boolean;
  mutation: UseMutationResult<Avtale, unknown, AvtaleRequest>;
  onLagreUtkast: () => void;
  mutationUtkast: UseMutationResult<Utkast, unknown, Utkast>;
}
export function AvtaleSkjemaKnapperadUtkast({
  onClose,
  avtale,
  utkastModus,
  mutation,
  onLagreUtkast,
  mutationUtkast,
}: Props) {
  const { data: slettAvtaleEnabled } = useFeatureToggle(
    Toggles.MULIGHETSROMMET_ADMIN_FLATE_SLETT_AVTALE,
  );
  const [avbrytModalOpen, setAvbrytModalOpen] = useState(false);
  const [sletteModalOpen, setSletteModalOpen] = useState(false);

  return utkastModus ? (
    <>
      <div className={styles.button_row}>
        {slettAvtaleEnabled ? (
          <SlettUtkastKnapp setSletteModal={setSletteModalOpen} />
        ) : null}
        <div>
          <LagreEndringerKnapp
            submit={false}
            onLagreUtkast={onLagreUtkast}
            mutationUtkast={mutationUtkast}
          />
          <OpprettAvtaleGjennomforingKnapp type="avtale" mutation={mutation} />
        </div>
      </div>

      <AvbrytAvtaleModal
        modalOpen={avbrytModalOpen}
        onClose={() => {
          setAvbrytModalOpen(false);
        }}
        avtale={avtale}
      />
      <SletteModal
        modalOpen={sletteModalOpen}
        onClose={() => setSletteModalOpen(false)}
        headerText="Ønsker du å slette utkastet?"
        headerTextError="Kan ikke slette notatet."
        handleDelete={onClose}
      />
    </>
  ) : null;
}
