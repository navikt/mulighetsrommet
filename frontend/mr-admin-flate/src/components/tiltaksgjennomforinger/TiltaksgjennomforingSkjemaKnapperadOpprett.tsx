import styles from "../skjema/Skjema.module.scss";
import React, { useState } from "react";
import { UseMutationResult } from "@tanstack/react-query";
import {
  Tiltaksgjennomforing,
  TiltaksgjennomforingRequest,
} from "mulighetsrommet-api-client";
import { SlettUtkastKnapp } from "../knapper/SlettUtkastKnapp";
import { LagreUtkastKnapp } from "../knapper/LagreUtkastKnapp";
import { OpprettAvtaleGjennomforingKnapp } from "../knapper/OpprettAvtaleGjennomforingKnapp";
import SletteModal from "../modal/SletteModal";
import { useMutateUtkast } from "../../api/utkast/useMutateUtkast";
import { useNavigate } from "react-router-dom";

interface Props {
  onClose: () => void;
  mutation: UseMutationResult<
    Tiltaksgjennomforing,
    unknown,
    TiltaksgjennomforingRequest,
    unknown
  >;
  onLagreUtkast: () => void;
  error: () => void;
}
export function TiltaksgjennomforingSkjemaKnapperadOpprett({
  onClose,
  mutation,
  onLagreUtkast,
  error,
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
