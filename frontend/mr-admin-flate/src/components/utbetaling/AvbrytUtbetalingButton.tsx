import { FieldError, UtbetalingDto, ValidationError } from "@mr/api-client-v2";
import { Button } from "@navikt/ds-react";
import { useState } from "react";
import { useNavigate } from "react-router";
import { useAvbrytUtbetaling } from "@/api/utbetaling/useAvbrytUtbetaling";
import { AarsakerOgForklaringModal } from "../modal/AarsakerOgForklaringModal";

export interface Props {
  utbetaling: UtbetalingDto;
  setErrors: (a: FieldError[]) => void;
}

export function AvbrytUtbetalingButton({ utbetaling, setErrors }: Props) {
  const [avbrytModalOpen, setAvbrytModalOpen] = useState<boolean>(false);
  const navigate = useNavigate();

  const avbrytMutation = useAvbrytUtbetaling();

  function avbryt(aarsaker: string[], forklaring: string | null) {
    avbrytMutation.mutate(
      {
        id: utbetaling.id,
        body: { aarsaker, forklaring },
      },
      {
        onValidationError: (error: ValidationError) => {
          setErrors(error.errors);
        },
        onSuccess: () => {
          setErrors([]);
          navigate(-1);
        },
      },
    );
  }

  return (
    <>
      <Button size="small" variant="danger" onClick={() => setAvbrytModalOpen(true)}>
        Avbryt utbetaling
      </Button>
      <AarsakerOgForklaringModal<"ANNET">
        open={avbrytModalOpen}
        header="Avbryt utbetaling"
        buttonLabel="Avbryt utbetaling"
        aarsaker={[{ value: "ANNET", label: "Annet" }]}
        onClose={() => setAvbrytModalOpen(false)}
        onConfirm={({ aarsaker, forklaring }) => {
          avbryt(aarsaker, forklaring);
        }}
      />
    </>
  );
}
