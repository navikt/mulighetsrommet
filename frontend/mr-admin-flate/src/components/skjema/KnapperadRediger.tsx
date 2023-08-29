import styles from "../skjema/Skjema.module.scss";
import React, { useEffect, useState } from "react";
import { UseMutationResult } from "@tanstack/react-query";
import { Utkast } from "mulighetsrommet-api-client";
import { SubmitSkjemaKnapp } from "../skjemaknapper/SubmitSkjemaKnapp";
import SletteModal from "../modal/SletteModal";
import { useDeleteUtkast } from "../../api/utkast/useDeleteUtkast";
import { useUtkast } from "../../api/utkast/useUtkast";
import { AvbrytKnapp } from "../skjemaknapper/AvbrytKnapp";

interface PropsOpprett {
  opprettMutation: UseMutationResult<any, unknown, any>;
  handleDelete: () => void;
  redigeringsmodus: boolean;
  mutationUtkast: UseMutationResult<Utkast, unknown, Utkast>;
  type: "avtale" | "gjennomføring";
  utkastmodus: boolean;
}
export function KnapperadRediger({
  opprettMutation,
  handleDelete,
  redigeringsmodus,
  mutationUtkast,
  type,
  utkastmodus,
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
      <div className={styles.knapperad_skjema_utkast}>
        {!utkastmodus && (
          <AvbrytKnapp
            setSlettemodal={() => setUtkastIdForSletting(utkastId)}
            dirtyForm={mutationUtkast.isSuccess}
          />
        )}
        <SubmitSkjemaKnapp
          type={type}
          mutation={opprettMutation}
          redigeringsmodus={redigeringsmodus}
          utkastmodus={utkastmodus}
        />
      </div>

      <SletteModal
        modalOpen={!!utkastIdForSletting}
        onClose={() => setUtkastIdForSletting(null)}
        headerText="Ønsker du å avbryte?"
        headerSubText="De siste endringene blir ikke lagret."
        headerTextError="Kan ikke slette utkastet."
        handleDelete={onDelete}
        mutation={mutationDeleteUtkast}
        avbryt
      />
    </>
  );
}
