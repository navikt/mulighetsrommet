import { useGodkjennAvbrytelseUtbetaling } from "@/api/utbetaling/mutations";
import { VarselModal } from "@mr/frontend-common/components/varsel/VarselModal";
import { BodyShort, Button } from "@navikt/ds-react";
import { FieldError, ValidationError } from "@tiltaksadministrasjon/api-client";

interface BekreftAvbrytelseModalProps {
  utbetalingId: string;
  setErrors: (errs: FieldError[]) => void;
  open: boolean;
  onClose: () => void;
}

export function BekreftAvbrytelseModal({
  utbetalingId,
  setErrors,
  open,
  onClose,
}: BekreftAvbrytelseModalProps) {
  const godkjennAvbyrtelseUtbetalingMutation = useGodkjennAvbrytelseUtbetaling();

  function godkjennAvbrytelseUtbetaling() {
    godkjennAvbyrtelseUtbetalingMutation.mutate(
      { id: utbetalingId },
      {
        onSuccess() {
          onClose();
        },
        onValidationError: (error: ValidationError) => {
          setErrors(error.errors);
        },
      },
    );
  }

  return (
    <VarselModal
      headingIconType="warning"
      headingText="Bekreft avbrytelse"
      open={open}
      handleClose={() => onClose()}
      body={
        <BodyShort>
          Du er i ferd med å avbryte en utbetaling. Arrangøren vil ikke få utbetalt noe for denne
          perioden. Er du sikker på at du vil fortsette?
        </BodyShort>
      }
      primaryButton={
        <Button title="Avbryt utbetaling" variant="primary" onClick={godkjennAvbrytelseUtbetaling}>
          Ja, jeg vil avbryte utbetalingen
        </Button>
      }
      secondaryButton
    />
  );
}
