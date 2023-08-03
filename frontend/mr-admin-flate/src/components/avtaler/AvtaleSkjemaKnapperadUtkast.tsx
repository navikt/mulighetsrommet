import styles from "../skjema/Skjema.module.scss";
import { useFeatureToggle } from "../../api/features/feature-toggles";
import { Avtale, Toggles } from "mulighetsrommet-api-client";
import AvbrytAvtaleModal from "./AvbrytAvtaleModal";
import { useState } from "react";
import { LagreEndringerKnapp } from "../knapper/LagreEndringerKnapp";
import { ForkastEndringerKnapp } from "../knapper/ForkastEndringerKnapp";
import { SlettUtkastKnapp } from "../knapper/SlettUtkastKnapp";
import SletteModal from "../modal/SletteModal";

interface Props {
  onClose: () => void;
  avtale: Avtale;
  utkastModus: boolean;
}
export function AvtaleSkjemaKnapperadUtkast({
  onClose,
  avtale,
  utkastModus,
}: Props) {
  const { data: slettAvtaleEnabled } = useFeatureToggle(
    Toggles.MULIGHETSROMMET_ADMIN_FLATE_SLETT_AVTALE,
  );
  const [avbrytModalOpen, setAvbrytModalOpen] = useState(false);
  const [sletteModalOpen, setSletteModalOpen] = useState(false);

  return utkastModus ? (
    <div className={styles.button_row}>
      {slettAvtaleEnabled ? (
        <SlettUtkastKnapp setSletteModal={setSletteModalOpen} />
      ) : null}
      <div>
        <ForkastEndringerKnapp type="avtale" onClose={onClose} />
        <LagreEndringerKnapp />
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
    </div>
  ) : null;
}
