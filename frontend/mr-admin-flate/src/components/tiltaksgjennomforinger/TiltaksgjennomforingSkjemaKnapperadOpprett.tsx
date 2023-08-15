import styles from "../skjema/Skjema.module.scss";
import React, { useState } from "react";
import { UseMutationResult } from "@tanstack/react-query";
import {
  Tiltaksgjennomforing,
  TiltaksgjennomforingRequest,
} from "mulighetsrommet-api-client";
import { SlettUtkastKnapp } from "../knapper/SlettUtkastKnapp";
import { OpprettAvtaleGjennomforingKnapp } from "../knapper/OpprettAvtaleGjennomforingKnapp";
import SletteModal from "../modal/SletteModal";
import { LagreEndringerKnapp } from "../knapper/LagreEndringerKnapp";
import { useMutateUtkast } from "../../api/utkast/useMutateUtkast";

interface Props {
  onClose: () => void;
  mutation: UseMutationResult<
    Tiltaksgjennomforing,
    unknown,
    TiltaksgjennomforingRequest,
    unknown
  >;
  onLagreUtkast: () => void;
}
export function TiltaksgjennomforingSkjemaKnapperadOpprett({
  onClose,
  mutation,
  onLagreUtkast,
}: Props) {
  const [sletteModal, setSletteModal] = useState(false);

  // const handleLagreUtkast = () => {
  //   if (mutationUtkast.error === 0) {
  //     onLagreUtkast();
  //     navigate(`/avtaler#avtaleOversiktTab="utkast"`);
  //   } else {
  //     error();
  //   }
  // };

  return (
    <div className={styles.button_row}>
      <SlettUtkastKnapp setSletteModal={() => setSletteModal(true)} />
      <div>
        <LagreEndringerKnapp
          onLagreUtkast={onLagreUtkast}
          dataTestId="tiltaksgjennomforingsskjema-lagre-utkast"
          submit={false}
          knappetekst="Lagre som utkast"
        />

        <OpprettAvtaleGjennomforingKnapp
          type="gjennomføring"
          mutation={mutation}
        />
      </div>
      <SletteModal
        modalOpen={sletteModal}
        onClose={() => setSletteModal(false)}
        headerText="Ønsker du å slette utkastet?"
        headerTextError="Kan ikke slette utkastet."
        handleDelete={onClose}
        mutation={useMutateUtkast()}
      />
    </div>
  );
}
