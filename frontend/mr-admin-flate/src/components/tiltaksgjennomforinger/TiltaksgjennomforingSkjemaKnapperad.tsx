import { Button } from "@navikt/ds-react";
import { UseMutationResult } from "@tanstack/react-query";
import { Tiltaksgjennomforing, TiltaksgjennomforingRequest } from "mulighetsrommet-api-client";
import { ValideringsfeilOppsummering } from "../skjema/ValideringsfeilOppsummering";
import { SkjemaKnapperad } from "@/components/skjema/SkjemaKnapperad";

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
    <SkjemaKnapperad>
      <ValideringsfeilOppsummering />
      <Button
        size="small"
        onClick={onClose}
        variant="tertiary"
        type="button"
        disabled={mutation.isPending}
      >
        Avbryt
      </Button>
      <Button size="small" type="submit" disabled={mutation.isPending}>
        {mutation.isPending ? "Lagrer..." : redigeringsModus ? "Lagre gjennomføring" : "Opprett"}
      </Button>
    </SkjemaKnapperad>
  );
}
