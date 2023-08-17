import styles from "../skjema/Skjema.module.scss";
import React, { useEffect, useState } from "react";
import { UseMutationResult } from "@tanstack/react-query";
import { Utkast } from "mulighetsrommet-api-client";
import { SubmitSkjemaKnapp } from "../skjemaknapper/SubmitSkjemaKnapp";
import SletteModal from "../modal/SletteModal";
import { SlettUtkastKnapp } from "../skjemaknapper/SlettUtkastKnapp";
import { useDeleteUtkast } from "../../api/utkast/useDeleteUtkast";
import { useUtkast } from "../../api/utkast/useUtkast";

interface PropsOpprett {
  opprettMutation: UseMutationResult<any, unknown, any, unknown>;
  handleDelete: () => void;
  redigeringsmodus: boolean;
  mutationUtkast: UseMutationResult<Utkast, unknown, Utkast>;
}
export function KnapperadOpprett({
  opprettMutation,
  handleDelete,
  redigeringsmodus,
  mutationUtkast,
}: PropsOpprett) {
  const [utkastIdForSletting, setUtkastIdForSletting] = useState<null | string>(
    null,
  );
  const mutationDeleteUtkast = useDeleteUtkast();
  const { refetch } = useUtkast();

  async function onDelete() {
    mutationDeleteUtkast.mutate(utkastIdForSletting!, {
      onSuccess: async () => {
        setUtkastIdForSletting(null);
        handleDelete();
        await refetch();
      },
    });
  }

  let utkastId: string | null = null;
  useEffect(() => {
    if (mutationUtkast.isSuccess) {
      utkastId = mutationUtkast.data.id;
    }
  }, [mutationUtkast.isSuccess]);

  return (
    <>
      <div className={styles.button_row}>
        <SlettUtkastKnapp
          setSlettemodal={() => setUtkastIdForSletting(utkastId)}
          disabled={!mutationUtkast.isSuccess}
        />
        <SubmitSkjemaKnapp
          type="gjennomføring"
          mutation={opprettMutation}
          dataTestId="lagre-opprett-knapp"
          redigeringsmodus={redigeringsmodus}
        />
      </div>

      <SletteModal
        modalOpen={!!utkastIdForSletting}
        onClose={() => setUtkastIdForSletting(null)}
        headerText="Ønsker du å slette utkastet?"
        headerTextError="Kan ikke slette utkastet."
        handleDelete={onDelete}
        mutation={mutationDeleteUtkast}
      />
    </>
  );
}
