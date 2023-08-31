import styles from "../skjema/Skjema.module.scss";
import React, { useEffect, useState } from "react";
import { UseMutationResult } from "@tanstack/react-query";
import { Utkast } from "mulighetsrommet-api-client";
import { SubmitSkjemaKnapp } from "../skjemaknapper/SubmitSkjemaKnapp";
import SletteModal from "../modal/SletteModal";
import { AvbrytKnapp } from "../skjemaknapper/AvbrytKnapp";
import { useDeleteUtkast } from "../../api/utkast/useDeleteUtkast";
import { useUtkast } from "../../api/utkast/useUtkast";

interface PropsOpprett {
  opprettMutation: UseMutationResult<any, unknown, any>;
  onClose: () => void;
  redigeringsmodus: boolean;
  mutationUtkast: UseMutationResult<Utkast, unknown, Utkast>;
  type: "avtale" | "gjennomføring";
}

export function KnapperadOpprett({
  opprettMutation,
  onClose,
  redigeringsmodus,
  mutationUtkast,
  type,
}: PropsOpprett) {
  const [utkastIdForSletting, setUtkastIdForSletting] = useState<null | string>(
    null,
  );
  const mutationDeleteUtkast = useDeleteUtkast();
  const { refetch } = useUtkast();

  async function slettUtkast() {
    if (!utkastIdForSletting) throw new Error("Fant ingen avtaleId");

    mutationDeleteUtkast.mutate(utkastIdForSletting!, {
      onSuccess: async () => {
        setUtkastIdForSletting(null);
        onClose();
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
      <div className={styles.knapperad_skjema}>
        <AvbrytKnapp
          setSlettemodal={() => setUtkastIdForSletting(utkastId)}
          dirtyForm={mutationUtkast.isSuccess}
        />
        <SubmitSkjemaKnapp
          type={type}
          mutation={opprettMutation}
          redigeringsmodus={redigeringsmodus}
        />
      </div>

      {utkastIdForSletting ? (
        <SletteModal
          modalOpen={!!utkastIdForSletting}
          onClose={() => setUtkastIdForSletting(null)}
          mutation={mutationDeleteUtkast}
          handleDelete={slettUtkast}
          headerText="Ønsker du å avbryte?"
          headerSubText="Utkastet blir ikke lagret."
          headerTextError="Kan ikke slette utkastet."
          avbryt
        />
      ) : null}
    </>
  );
}
