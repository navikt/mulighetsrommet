import styles from "../skjema/Skjema.module.scss";
import React, { useState } from "react";
import { UseMutationResult } from "@tanstack/react-query";
import {
  Tiltaksgjennomforing,
  TiltaksgjennomforingRequest,
  Utkast,
} from "mulighetsrommet-api-client";
import { SlettUtkastKnapp } from "../knapper/SlettUtkastKnapp";
import { LagreUtkastKnapp } from "../knapper/LagreUtkastKnapp";
import { OpprettAvtaleGjennomforingKnapp } from "../knapper/OpprettAvtaleGjennomforingKnapp";
import SletteModal from "../modal/SletteModal";

interface Props {
  onClose: () => void;
  mutation: UseMutationResult<
    Tiltaksgjennomforing,
    unknown,
    TiltaksgjennomforingRequest,
    unknown
  >;
  onLagreUtkast: () => void;
  mutationUtkast: UseMutationResult<Utkast, unknown, Utkast>;
}
export function TiltaksgjennomforingSkjemaKnapperadOpprett({
  onClose,
  mutation,
  onLagreUtkast,
  mutationUtkast,
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
      <SlettUtkastKnapp setSletteModal={setSletteModal} />
      <div>
        <LagreUtkastKnapp
          dataTestId="tiltaksgjennomforingsskjema-lagre-utkast"
          onLagreUtkast={onLagreUtkast}
          mutationUtkast={mutationUtkast}
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
      />
    </div>
  );
}
