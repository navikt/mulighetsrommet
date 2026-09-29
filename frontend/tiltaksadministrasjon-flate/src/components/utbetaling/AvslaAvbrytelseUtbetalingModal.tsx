import {
  UtbetalingStatusAarsak,
  FieldError,
  ValidationError,
  AarsakerOgBegrunnelseRequestUtbetalingStatusAarsak,
} from "@tiltaksadministrasjon/api-client";
import { useAvslaAvbrytelseUtbetaling } from "@/api/utbetaling/mutations";
import { AarsakerOgBegrunnelseModal } from "@/components/modal/AarsakerOgBegrunnelseModal";
import { useState } from "react";
import { utbetalingTekster } from "@/components/utbetaling/UtbetalingTekster";

interface AvslaAvbrytelseUtbetalingModalProps {
  utbetalingId: string;
  open: boolean;
  onClose: () => void;
}

export function AvslaAvbrytelseUtbetalingModal({
  utbetalingId,
  open,
  onClose,
}: AvslaAvbrytelseUtbetalingModalProps) {
  const [errors, setErrors] = useState<FieldError[]>([]);
  const avslaAvbrytelseMutation = useAvslaAvbrytelseUtbetaling();

  function avslaAvbrytelseUtbetaling(body: AarsakerOgBegrunnelseRequestUtbetalingStatusAarsak) {
    avslaAvbrytelseMutation.mutate(
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

  const avsalgAarsakValg = [UtbetalingStatusAarsak.ANNET].map((val) => {
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
      header={utbetalingTekster.avbrutt.modal.avsla.header}
      aarsaker={avsalgAarsakValg}
      buttonLabel={utbetalingTekster.avbrutt.modal.avsla.button.label}
      errors={errors}
      onConfirm={(request) => avslaAvbrytelseUtbetaling(request)}
    />
  );
}
