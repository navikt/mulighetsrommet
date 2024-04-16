import { Button } from "@navikt/ds-react";
import { UseMutationResult } from "@tanstack/react-query";
import { Tiltaksgjennomforing, TiltaksgjennomforingRequest } from "mulighetsrommet-api-client";
import styles from "../skjema/Skjema.module.scss";
import { ValideringsfeilOppsummering } from "../skjema/ValideringsfeilOppsummering";

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
    <div>
      <ValideringsfeilOppsummering />
      <Button
        size="small"
        className={styles.button}
        onClick={onClose}
        variant="tertiary"
        type="button"
        disabled={mutation.isPending}
      >
        Avbryt
      </Button>
      <Button size="small" className={styles.button} type="submit" disabled={mutation.isPending}>
        {mutation.isPending ? "Lagrer..." : redigeringsModus ? "Lagre gjennomføring" : "Opprett"}
      </Button>
    </div>
  );
}
