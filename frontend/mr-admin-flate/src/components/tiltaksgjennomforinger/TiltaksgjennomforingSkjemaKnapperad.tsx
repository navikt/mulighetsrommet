import { Button, HStack } from "@navikt/ds-react";
import { UseMutationResult } from "@tanstack/react-query";
import {
  Tiltaksgjennomforing,
  TiltaksgjennomforingRequest,
  UtkastDto as Utkast,
  UtkastRequest,
} from "mulighetsrommet-api-client";
import styles from "../skjema/Skjema.module.scss";
import { ValideringsfeilOppsummering } from "../skjema/ValideringsfeilOppsummering";
import { AutoSaveUtkast } from "../autosave/AutoSaveUtkast";

interface Props {
  redigeringsModus: boolean;
  onClose: () => void;
  mutation: UseMutationResult<Tiltaksgjennomforing, unknown, TiltaksgjennomforingRequest, unknown>;
  size?: "small" | "medium";
  defaultValues: any;
  utkastIdRef: string;
  onSave: () => void;
  mutationUtkast: UseMutationResult<Utkast, unknown, UtkastRequest>;
  lagreState?: string;
  setLagreState: (state: string) => void;
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
  lagreState,
  setLagreState,
}: Props) {
  return (
    <HStack align="center" className={styles.knapperad}>
      <AutoSaveUtkast
        defaultValues={defaultValues}
        utkastId={utkastIdRef}
        onSave={onSave}
        mutationUtkast={mutationUtkast}
        lagreState={lagreState}
        setLagreState={setLagreState}
      />
      <ValideringsfeilOppsummering />
      <Button
        size={size}
        className={styles.button}
        onClick={onClose}
        variant="tertiary"
        type="button"
        disabled={mutation.isPending}
      >
        Avbryt
      </Button>
      <Button size={size} className={styles.button} type="submit" disabled={mutation.isPending}>
        {mutation.isPending ? "Lagrer..." : redigeringsModus ? "Lagre gjennomføring" : "Opprett"}
      </Button>
    </HStack>
  );
}
