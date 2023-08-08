import styles from "../skjema/Skjema.module.scss";
import { useState } from "react";
import SletteModal from "../modal/SletteModal";
import { SlettUtkastKnapp } from "../knapper/SlettUtkastKnapp";
import { LagreUtkastKnapp } from "../knapper/LagreUtkastKnapp";
import { OpprettAvtaleGjennomforingKnapp } from "../knapper/OpprettAvtaleGjennomforingKnapp";
import { UseMutationResult } from "@tanstack/react-query";
import { Avtale, AvtaleRequest, Utkast } from "mulighetsrommet-api-client";

interface Props {
  onClose: () => void;
  mutation: UseMutationResult<Avtale, unknown, AvtaleRequest>;
  mutationUtkast: UseMutationResult<Utkast, unknown, Utkast>;
  onLagreUtkast: () => void;
}
export function AvtaleSkjemaKnapperadOpprett({
  onClose,
  mutation,
  onLagreUtkast,
  mutationUtkast,
}: Props) {
  const [sletteModal, setSletteModal] = useState(false);

  return (
    <div className={styles.button_row}>
      <SlettUtkastKnapp setSletteModal={setSletteModal} />
      <div>
        <LagreUtkastKnapp
          dataTestId="avtaleskjema-lagre-utkast"
          onLagreUtkast={onLagreUtkast}
          mutationUtkast={mutationUtkast}
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
