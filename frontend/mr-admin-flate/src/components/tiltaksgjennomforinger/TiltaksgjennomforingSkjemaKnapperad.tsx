import { Button, HStack } from "@navikt/ds-react";
import { UseMutationResult } from "@tanstack/react-query";
import {
  Tiltaksgjennomforing,
  TiltaksgjennomforingRequest,
  Utkast,
} from "mulighetsrommet-api-client";
import { AutoSaveUtkast } from "../autosave/AutoSaveUtkast";
import React from "react";
import styles from "../skjema/Skjema.module.scss";
import { ValideringsfeilOppsummering } from "../skjema/ValideringsfeilOppsummering";

interface Props {
  redigeringsModus: boolean;
  onClose: () => void;
  mutation: UseMutationResult<Tiltaksgjennomforing, unknown, TiltaksgjennomforingRequest, unknown>;
  size?: "small" | "medium";
  defaultValues: any;
  utkastIdRef: string;
  onSave: () => void;
  mutationUtkast: UseMutationResult<Utkast, unknown, Utkast>;
}
export function TiltaksgjennomforingSkjemaKnapperad({
  redigeringsModus,
  onClose,
  mutation,
  size = "medium",
  defaultValues,
  utkastIdRef,
  onSave,
  mutationUtkast,
}: Props) {
  return (
    <HStack align="center" className={styles.knapperad}>
      <AutoSaveUtkast
        defaultValues={defaultValues}
        utkastId={utkastIdRef}
        onSave={onSave}
        mutationUtkast={mutationUtkast}
      />
      <ValideringsfeilOppsummering />
      <Button
        size={size}
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
        size={size}
        className={styles.button}
        type="submit"
        disabled={mutation.isLoading}
        data-testid="lagre-opprett-knapp"
      >
        {mutation.isLoading ? "Lagrer..." : redigeringsModus ? "Lagre gjennomføring" : "Opprett"}
      </Button>
    </HStack>
  );
}
