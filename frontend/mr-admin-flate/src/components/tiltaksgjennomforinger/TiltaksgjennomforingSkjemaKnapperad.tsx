import styles from "../skjema/Skjema.module.scss";
import { Button } from "@navikt/ds-react";
import React from "react";
import { UseMutationResult } from "@tanstack/react-query";
import { Tiltaksgjennomforing, TiltaksgjennomforingRequest } from "mulighetsrommet-api-client";

interface Props {
  redigeringsModus: boolean;
  onClose: () => void;
  mutation: UseMutationResult<Tiltaksgjennomforing, unknown, TiltaksgjennomforingRequest, unknown>;
}
export function TiltaksgjennomforingSkjemaKnapperad({
  redigeringsModus,
  onClose,
  mutation,
}: Props) {
  return (
    <div className={styles.button_row}>
      <Button
        className={styles.button}
        onClick={onClose}
        variant="tertiary"
        type="button"
        data-testid="avbryt-knapp"
        disabled={mutation.isLoading}
      >
        Avbryt
      </Button>
      <Button
        className={styles.button}
        type="submit"
        disabled={mutation.isLoading}
        data-testid="lagre-opprett-knapp"
      >
        {mutation.isLoading ? "Lagrer..." : redigeringsModus ? "Lagre gjennomføring" : "Opprett"}
      </Button>
    </div>
  );
}
