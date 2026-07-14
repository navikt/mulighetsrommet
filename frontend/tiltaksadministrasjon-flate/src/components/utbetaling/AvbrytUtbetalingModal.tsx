import {
  UtbetalingStatusAarsak,
  FieldError,
  ValidationError,
  AarsakerOgForklaringRequestUtbetalingStatusAarsak,
} from "@tiltaksadministrasjon/api-client";
import { useAvbrytUtbetaling } from "@/api/utbetaling/mutations";
import { AarsakerOgForklaringModal } from "@/components/modal/AarsakerOgForklaringModal";
import { useState } from "react";
import { utbetalingTekster } from "./UtbetalingTekster";
import { BodyShort } from "@navikt/ds-react";

interface AvbrytUtbetalingModalProps {
  utbetalingId: string;
  open: boolean;
  onClose: () => void;
}

export function AvbrytUtbetalingModal({ utbetalingId, open, onClose }: AvbrytUtbetalingModalProps) {
  const [errors, setErrors] = useState<FieldError[]>([]);
  const avbrytUtbetalingMutation = useAvbrytUtbetaling();

  function avbrytUtbetaling(body: AarsakerOgForklaringRequestUtbetalingStatusAarsak) {
    avbrytUtbetalingMutation.mutate(
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

  const avbrytUtbetalingAarsakValg = [
    UtbetalingStatusAarsak.TILSAGN_GJORT_OPP,
    UtbetalingStatusAarsak.ANNET,
  ].map((val) => {
    return {
      value: val,
      label: utbetalingTekster.avbrutt.aarsak.fraAarsak(val),
    };
  });
  return (
    <AarsakerOgForklaringModal<UtbetalingStatusAarsak>
      width={750}
      open={open}
      onClose={onClose}
      header={utbetalingTekster.avbrutt.aarsak.modal.header}
      ingress={<BodyShort>{utbetalingTekster.avbrutt.aarsak.modal.ingress}</BodyShort>}
      aarsaker={avbrytUtbetalingAarsakValg}
      buttonLabel={utbetalingTekster.avbrutt.aarsak.modal.button.label}
      errors={errors}
      onConfirm={(request) => avbrytUtbetaling(request)}
    />
  );
}
