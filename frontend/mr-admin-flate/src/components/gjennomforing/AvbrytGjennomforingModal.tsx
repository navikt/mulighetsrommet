import { Alert } from "@navikt/ds-react";
import { useState } from "react";
import { useAvbrytGjennomforing } from "@/api/gjennomforing/useAvbrytGjennomforing";
import { AarsakerOgForklaringModal } from "@/components/modal/AarsakerOgForklaringModal";
import { useGjennomforingDeltakerSummary } from "@/api/gjennomforing/useGjennomforingDeltakerSummary";
import {
  AvbrytGjennomforingAarsak,
  FieldError,
  ValidationError,
} from "@tiltaksadministrasjon/api-client";
import { useGjennomforing } from "@/api/gjennomforing/useGjennomforing";
import { avbrytGjennomforingAarsakTilTekst } from "@/utils/Utils";

interface AvbrytGjennomforingModalProps {
  open: boolean;
  setOpen: (open: boolean) => void;
  gjennomforingId: string;
}

export function AvbrytGjennomforingModal({
  open,
  setOpen,
  gjennomforingId,
}: AvbrytGjennomforingModalProps) {
  const { gjennomforing } = useGjennomforing(gjennomforingId);
  const avbrytMutation = useAvbrytGjennomforing();
  const { data: deltakerSummary } = useGjennomforingDeltakerSummary(gjennomforingId);

  const [avbrytModalErrors, setAvbrytModalErrors] = useState<FieldError[]>([]);

  function avbryt(aarsaker: AvbrytGjennomforingAarsak[], forklaring: string | null) {
    avbrytMutation.mutate(
      {
        id: gjennomforing.id,
        aarsaker,
        forklaring,
      },
      {
        onSuccess: () => {
          setOpen(false);
        },
        onValidationError: (error: ValidationError) => {
          setAvbrytModalErrors(error.errors);
        },
      },
    );
  }

  return (
    <AarsakerOgForklaringModal<AvbrytGjennomforingAarsak>
      header={`Ønsker du å avbryte «${gjennomforing.navn}»?`}
      open={open}
      buttonLabel="Ja, jeg vil avbryte gjennomføringen"
      ingress={
        deltakerSummary.antallDeltakere > 0 && (
          <Alert variant="warning">
            {`Det finnes ${deltakerSummary.antallDeltakere} deltaker${deltakerSummary.antallDeltakere > 1 ? "e" : ""} på gjennomføringen. Ved å
           avbryte denne vil det føre til statusendring på alle deltakere som har en aktiv status.`}
          </Alert>
        )
      }
      aarsaker={[
        {
          value: AvbrytGjennomforingAarsak.BUDSJETT_HENSYN,
          label: avbrytGjennomforingAarsakTilTekst(AvbrytGjennomforingAarsak.BUDSJETT_HENSYN),
        },
        {
          value: AvbrytGjennomforingAarsak.ENDRING_HOS_ARRANGOR,
          label: avbrytGjennomforingAarsakTilTekst(AvbrytGjennomforingAarsak.ENDRING_HOS_ARRANGOR),
        },
        {
          value: AvbrytGjennomforingAarsak.FEILREGISTRERING,
          label: avbrytGjennomforingAarsakTilTekst(AvbrytGjennomforingAarsak.FEILREGISTRERING),
        },
        {
          value: AvbrytGjennomforingAarsak.FOR_FAA_DELTAKERE,
          label: avbrytGjennomforingAarsakTilTekst(AvbrytGjennomforingAarsak.FOR_FAA_DELTAKERE),
        },
        {
          value: AvbrytGjennomforingAarsak.AVBRUTT_I_ARENA,
          label: avbrytGjennomforingAarsakTilTekst(AvbrytGjennomforingAarsak.AVBRUTT_I_ARENA),
        },
        {
          value: AvbrytGjennomforingAarsak.ANNET,
          label: avbrytGjennomforingAarsakTilTekst(AvbrytGjennomforingAarsak.ANNET),
        },
      ]}
      onClose={() => {
        setOpen(false);
        setAvbrytModalErrors([]);
      }}
      onConfirm={({ aarsaker, forklaring }) => avbryt(aarsaker, forklaring)}
      errors={avbrytModalErrors}
    />
  );
}
