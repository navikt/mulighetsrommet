import styles from "../skjema/Skjema.module.scss";
import React, { useState } from "react";
import { UseMutationResult } from "@tanstack/react-query";
import {
  Tiltaksgjennomforing,
  TiltaksgjennomforingRequest,
  Utkast,
} from "mulighetsrommet-api-client";
import { SubmitSkjemaKnapp } from "../skjemaknapper/SubmitSkjemaKnapp";
import SletteModal from "../modal/SletteModal";
import { SlettUtkastKnapp } from "../skjemaknapper/SlettUtkastKnapp";
import { useDeleteUtkast } from "../../api/utkast/useDeleteUtkast";

interface PropsOpprett {
  opprettMutation: UseMutationResult<
    Tiltaksgjennomforing,
    unknown,
    TiltaksgjennomforingRequest,
    unknown
  >;
  handleDelete: () => void;
  redigeringsmodus: boolean;
  mutationUtkast: UseMutationResult<Utkast, unknown, Utkast>;
}
export function TiltaksgjennomforingSkjemaKnapperadOpprett({
  opprettMutation,
  handleDelete,
  redigeringsmodus,
  mutationUtkast,
}: PropsOpprett) {
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
          type="gjennomføring"
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
