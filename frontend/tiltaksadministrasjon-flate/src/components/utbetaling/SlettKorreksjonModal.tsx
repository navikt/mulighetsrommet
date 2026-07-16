import { useSlettKorreksjon } from "@/api/utbetaling/mutations";
import { VarselModal } from "@mr/frontend-common/components/varsel/VarselModal";
import { BodyShort, Button } from "@navikt/ds-react";
import { useNavigate } from "react-router";

interface SlettKorreksjonModalProps {
  utbetalingId: string;
  open: boolean;
  onClose: () => void;
}

export function SlettKorreksjonModal({ utbetalingId, open, onClose }: SlettKorreksjonModalProps) {
  const navigate = useNavigate();
  const slettKorreksjonMutation = useSlettKorreksjon();

  function slettKorreksjon() {
    slettKorreksjonMutation.mutate(
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
          Du er i ferd med å slette en korrigeringsutbetaling. Dette vil fjerne den valgte
          ubetalingen fra løsningen. Er du sikker på at du vil fortsette?
        </BodyShort>
      }
      primaryButton={
        <Button
          data-color="danger"
          title="Slett utbetaling"
          variant="primary"
          onClick={slettKorreksjon}
        >
          Ja, jeg vil slette utbetalingen
        </Button>
      }
      secondaryButton
    />
  );
}
