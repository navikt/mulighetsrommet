import styles from "../skjema/Skjema.module.scss";
import { useState } from "react";
import SletteModal from "../modal/SletteModal";
import { SlettUtkastKnapp } from "../knapper/SlettUtkastKnapp";
import { OpprettAvtaleGjennomforingKnapp } from "../knapper/OpprettAvtaleGjennomforingKnapp";
import { UseMutationResult } from "@tanstack/react-query";
import { Avtale, AvtaleRequest } from "mulighetsrommet-api-client";
import { LagreEndringerKnapp } from "../knapper/LagreEndringerKnapp";
import { useMutateUtkast } from "../../api/utkast/useMutateUtkast";

interface Props {
  handleDelete: () => void;
  mutation: UseMutationResult<Avtale, unknown, AvtaleRequest>;
  onLagreUtkast: () => void;
}
export function AvtaleSkjemaKnapperadOpprett({
  handleDelete,
  mutation,
  onLagreUtkast,
}: Props) {
  const [sletteModal, setSletteModal] = useState(false);

  return (
    <div className={styles.button_row}>
      <SlettUtkastKnapp setSletteModal={() => setSletteModal(true)} />
      <div>
        <LagreEndringerKnapp
          onLagreUtkast={onLagreUtkast}
          dataTestId="avtaleskjema-lagre-utkast"
          submit={false}
          knappetekst="Lagre som utkast"
        />
        <OpprettAvtaleGjennomforingKnapp type="avtale" mutation={mutation} />
      </div>
      <SletteModal
        modalOpen={sletteModal}
        onClose={() => setSletteModal(false)}
        headerText="Ønsker du å slette utkastet?"
        headerTextError="Kan ikke slette utkastet."
        handleDelete={handleDelete}
        mutation={useMutateUtkast()}
      />
    </div>
  );
}
