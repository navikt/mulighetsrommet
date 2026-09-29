import { useSlettUtbetaling } from "@/api/utbetaling/mutations";
import { VarselModal } from "@mr/frontend-common/components/varsel/VarselModal";
import { BodyShort, Button } from "@navikt/ds-react";
import { useNavigate } from "react-router";

interface SlettUtbetalingModalProps {
  utbetalingId: string;
  open: boolean;
  onClose: () => void;
}

export function SlettUtbetalingModal({ utbetalingId, open, onClose }: SlettUtbetalingModalProps) {
  const navigate = useNavigate();
  const slettUtbetalingMutation = useSlettUtbetaling();

  function slettUtbetaling() {
    slettUtbetalingMutation.mutate(
      { id: utbetalingId },
      {
        onSuccess: () => navigate("..", { replace: true }),
      },
    );
  }
  return (
    <VarselModal
      headingIconType="warning"
      headingText="Slett utbetaling"
      open={open}
      handleClose={() => onClose()}
      body={
        <BodyShort>
          Du er i ferd med å slette en utbetaling. Dette vil fjerne den valgte utbetalingen fra
          løsningen. Er du sikker på at du vil fortsette?
        </BodyShort>
      }
      primaryButton={
        <Button
          data-color="danger"
          title="Slett utbetaling"
          variant="primary"
          onClick={slettUtbetaling}
        >
          Ja, jeg vil slette utbetalingen
        </Button>
      }
      secondaryButton
    />
  );
}
