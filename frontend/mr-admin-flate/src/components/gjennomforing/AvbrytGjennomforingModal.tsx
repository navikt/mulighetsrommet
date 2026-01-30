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
        { value: AvbrytGjennomforingAarsak.BUDSJETT_HENSYN, label: "Budsjetthensyn" },
        { value: AvbrytGjennomforingAarsak.ENDRING_HOS_ARRANGOR, label: "Endring hos arrangør" },
        { value: AvbrytGjennomforingAarsak.FEILREGISTRERING, label: "Feilregistrering" },
        { value: AvbrytGjennomforingAarsak.FOR_FAA_DELTAKERE, label: "For få deltakere" },
        { value: AvbrytGjennomforingAarsak.AVBRUTT_I_ARENA, label: "Avbrutt i Arena" },
        { value: AvbrytGjennomforingAarsak.ANNET, label: "Annet" },
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
