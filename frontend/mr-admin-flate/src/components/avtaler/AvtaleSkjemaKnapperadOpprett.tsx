import styles from "../skjema/Skjema.module.scss";
import { useNavigate } from "react-router-dom";
import { useMutateUtkast } from "../../api/utkast/useMutateUtkast";
import { useState } from "react";
import SletteModal from "../modal/SletteModal";
import { SlettUtkastKnapp } from "../knapper/SlettUtkastKnapp";
import { LagreUtkastKnapp } from "../knapper/LagreUtkastKnapp";
import { OpprettAvtaleGjennomforingKnapp } from "../knapper/OpprettAvtaleGjennomforingKnapp";
import { UseMutationResult } from "@tanstack/react-query";
import { Avtale, AvtaleRequest } from "mulighetsrommet-api-client";

interface Props {
  onClose: () => void;
  onLagreUtkast: () => void;
  error: () => void;
  mutation: UseMutationResult<Avtale, unknown, AvtaleRequest, unknown>;
}
export function AvtaleSkjemaKnapperadOpprett({
  onLagreUtkast,
  error,
  onClose,
  mutation,
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

        <OpprettAvtaleGjennomforingKnapp type="avtale" mutation={mutation} />
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
