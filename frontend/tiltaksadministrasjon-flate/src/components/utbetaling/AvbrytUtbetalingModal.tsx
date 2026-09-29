import {
  UtbetalingStatusAarsak,
  FieldError,
  ValidationError,
  AarsakerOgBegrunnelseRequestUtbetalingStatusAarsak,
} from "@tiltaksadministrasjon/api-client";
import { useAvbrytUtbetaling } from "@/api/utbetaling/mutations";
import { AarsakerOgBegrunnelseModal } from "@/components/modal/AarsakerOgBegrunnelseModal";
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

  function avbrytUtbetaling(body: AarsakerOgBegrunnelseRequestUtbetalingStatusAarsak) {
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
      label: utbetalingTekster.avbrutt.fraAarsak(val),
    };
  });
  return (
    <AarsakerOgBegrunnelseModal<UtbetalingStatusAarsak>
      width={750}
      open={open}
      onClose={onClose}
      header={utbetalingTekster.avbrutt.modal.sendTilAvbrytning.header}
      ingress={<BodyShort>{utbetalingTekster.avbrutt.modal.sendTilAvbrytning.ingress}</BodyShort>}
      aarsaker={avbrytUtbetalingAarsakValg}
      buttonLabel={utbetalingTekster.avbrutt.modal.sendTilAvbrytning.button.label}
      textareaLabel="Begrunnelse til arrangør"
      errors={errors}
      onConfirm={(request) => avbrytUtbetaling(request)}
    />
  );
}
