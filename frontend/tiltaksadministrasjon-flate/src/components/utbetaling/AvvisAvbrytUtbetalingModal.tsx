import {
  UtbetalingStatusAarsak,
  FieldError,
  ValidationError,
  AarsakerOgForklaringRequestUtbetalingStatusAarsak,
} from "@tiltaksadministrasjon/api-client";
import { useAvvisAvbrytUtbetaling } from "@/api/utbetaling/mutations";
import { AarsakerOgForklaringModal } from "@/components/modal/AarsakerOgForklaringModal";
import { useState } from "react";
import { utbetalingTekster } from "./UtbetalingTekster";

interface AvvisAvbrytUtbetalingModalProps {
  utbetalingId: string;
  open: boolean;
  onClose: () => void;
}

export function AvvisAvbrytUtbetalingModal({
  utbetalingId,
  open,
  onClose,
}: AvvisAvbrytUtbetalingModalProps) {
  const [errors, setErrors] = useState<FieldError[]>([]);
  const avvisAvbrytningMutation = useAvvisAvbrytUtbetaling();

  function avvisAvbrytUtbetaling(body: AarsakerOgForklaringRequestUtbetalingStatusAarsak) {
    avvisAvbrytningMutation.mutate(
      { id: utbetalingId, body },
      {
        onValidationError: (error: ValidationError) => {
          setErrors(error.errors);
        },
        onSuccess: () => {
          onClose();
        },
      },
    );
  }

  const avvisAarsakValg = [UtbetalingStatusAarsak.ANNET].map((val) => {
    return {
      value: val,
      label: utbetalingTekster.avbrutt.fraAarsak(val),
    };
  });

  return (
    <AarsakerOgForklaringModal<UtbetalingStatusAarsak>
      width={750}
      open={open}
      onClose={onClose}
      header={utbetalingTekster.avbrutt.modal.avvis.header}
      aarsaker={avvisAarsakValg}
      buttonLabel={utbetalingTekster.avbrutt.modal.avvis.button.label}
      errors={errors}
      onConfirm={(request) => avvisAvbrytUtbetaling(request)}
    />
  );
}
