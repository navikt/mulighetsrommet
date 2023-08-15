import styles from "../skjema/Skjema.module.scss";
import { SubmitSkjemaKnapp } from "../skjemaknapper/SubmitSkjemaKnapp";
import { SlettUtkastKnapp } from "../skjemaknapper/SlettUtkastKnapp";
import { useState } from "react";
import SletteModal from "../modal/SletteModal";
import { useDeleteUtkast } from "../../api/utkast/useDeleteUtkast";
import { UseMutationResult } from "@tanstack/react-query";
import { Avtale, AvtaleRequest, Utkast } from "mulighetsrommet-api-client";

interface OpprettProps {
  opprettMutation: UseMutationResult<Avtale, unknown, AvtaleRequest>;
  handleDelete: () => void;
  redigeringsmodus: boolean;
  mutationUtkast: UseMutationResult<Utkast, unknown, Utkast>;
}

export function AvtaleSkjemaKnapperadOpprett({
  opprettMutation,
  handleDelete,
  redigeringsmodus,
  mutationUtkast,
}: OpprettProps) {
  const [slettemodal, setSlettemodal] = useState(false);
  const mutationDeleteUtkast = useDeleteUtkast();

  return (
    <>
      <div className={styles.button_row}>
        <SlettUtkastKnapp
          setSlettemodal={() => setSlettemodal(true)}
          disabled={!mutationUtkast.isSuccess}
        />
        <SubmitSkjemaKnapp
          type="avtale"
          mutation={opprettMutation}
          dataTestId="lagre-opprett-knapp"
          redigeringsmodus={redigeringsmodus}
        />
      </div>

      <SletteModal
        modalOpen={slettemodal}
        onClose={() => setSlettemodal(false)}
        headerText="Ønsker du å slette utkastet?"
        headerTextError="Kan ikke slette utkastet."
        handleDelete={handleDelete}
        mutation={mutationDeleteUtkast}
      />
    </>
  );
}
