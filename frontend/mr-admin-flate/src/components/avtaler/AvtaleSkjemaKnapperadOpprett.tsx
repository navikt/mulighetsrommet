import styles from "../skjema/Skjema.module.scss";
import { useNavigate } from "react-router-dom";
import { useMutateUtkast } from "../../api/utkast/useMutateUtkast";
import { useState } from "react";
import SletteModal from "../modal/SletteModal";
import { SlettUtkastKnapp } from "../knapper/SlettUtkastKnapp";
import { LagreUtkastKnapp } from "../knapper/LagreUtkastKnapp";
import { OpprettAvtaleGjennomforingKnapp } from "../knapper/OpprettAvtaleGjennomforingKnapp";

interface Props {
  onClose: () => void;
  onLagreUtkast: () => void;
  error: () => void;
}
export function AvtaleSkjemaKnapperadOpprett({
  onLagreUtkast,
  error,
  onClose,
}: Props) {
  const navigate = useNavigate();
  const mutationUtkast = useMutateUtkast();
  const [sletteModal, setSletteModal] = useState(false);

  //Todo funker ikke helt
  const handleLagreUtkast = () => {
    if (mutationUtkast.error === 0) {
      onLagreUtkast();
      navigate(`/avtaler#avtaleOversiktTab="utkast"`);
    } else {
      error();
    }
  };

  return (
    <div className={styles.button_row}>
      <SlettUtkastKnapp setSletteModal={setSletteModal} />
      <div>
        <LagreUtkastKnapp
          onLagreUtkast={handleLagreUtkast}
          type="avtale"
          dataTestId="avtaleskjema-lagre-utkast"
        />

        <OpprettAvtaleGjennomforingKnapp type="avtale" />
      </div>
      <SletteModal
        modalOpen={sletteModal}
        onClose={() => setSletteModal(false)}
        headerText="Ønsker du å slette utkastet?"
        headerTextError="Kan ikke slette notatet."
        handleDelete={onClose}
      />
    </div>
  );
}
