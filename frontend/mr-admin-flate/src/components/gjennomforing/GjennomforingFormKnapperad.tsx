import { Button } from "@navikt/ds-react";
import { ValideringsfeilOppsummering } from "../skjema/ValideringsfeilOppsummering";
import { SkjemaKnapperad } from "@/components/skjema/SkjemaKnapperad";

interface Props {
  redigeringsModus: boolean;
  onClose: () => void;
  isPending: boolean;
}
export function GjennomforingFormKnapperad({ redigeringsModus, onClose, isPending }: Props) {
  return (
    <SkjemaKnapperad>
      <ValideringsfeilOppsummering />
      <Button size="small" onClick={onClose} variant="tertiary" type="button" disabled={isPending}>
        Avbryt
      </Button>
      <Button size="small" type="submit" disabled={isPending}>
        {isPending ? "Lagrer..." : redigeringsModus ? "Lagre gjennomføring" : "Opprett"}
      </Button>
    </SkjemaKnapperad>
  );
}
