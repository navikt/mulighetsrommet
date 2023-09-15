import styles from "../skjema/Skjema.module.scss";
import React, { useEffect, useRef, useState } from "react";
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
  const utkastIdRef = useRef<null | string>();

  const [sletteModal, setSletteModal] = useState(false);

  const mutationDeleteUtkast = useDeleteUtkast();
  const { refetch } = useUtkast();

  useEffect(() => {
    if (mutationUtkast.isSuccess) {
      utkastIdRef.current = mutationUtkast.data.id;
    }
  }, [mutationUtkast.isSuccess]);

  async function slettUtkast() {
    if (!utkastIdRef.current) throw new Error("Fant ingen utkastId");

    mutationDeleteUtkast.mutate(utkastIdRef.current, {
      onSuccess: async () => {
        setSletteModal(false);
        onClose();
        await refetch();
      },
    });
  }

  return (
    <>
      <div className={styles.knapperad_skjema}>
        <AvbrytKnapp
          setSlettemodal={() => setSletteModal(true)}
          dirtyForm={mutationUtkast.isSuccess}
        />
        <SubmitSkjemaKnapp
          type={type}
          mutation={opprettMutation}
          redigeringsmodus={redigeringsmodus}
        />
      </div>

      <SletteModal
        modalOpen={sletteModal}
        onClose={() => setSletteModal(false)}
        mutation={mutationDeleteUtkast}
        handleDelete={slettUtkast}
        headerText="Ønsker du å avbryte?"
        headerSubText="Utkastet blir ikke lagret."
        headerTextError="Kan ikke slette utkastet."
        avbryt
      />
    </>
  );
}
