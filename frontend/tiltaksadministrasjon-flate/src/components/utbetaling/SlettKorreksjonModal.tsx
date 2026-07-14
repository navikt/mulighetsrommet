import { useSlettKorreksjon } from "@/api/utbetaling/mutations";
import { BodyShort, Button, Heading, HStack, Modal } from "@navikt/ds-react";
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
    <Modal onClose={onClose} closeOnBackdropClick aria-label="modal" open={open}>
      <Modal.Header closeButton={false}>
        <Heading align="start" size="medium">
          Slett utbetaling
        </Heading>
      </Modal.Header>
      <Modal.Body>
        <BodyShort>
          Du er i ferd med å slette en korrigeringsutbetaling. Dette vil fjerne den valgte
          ubetalingen fra løsningen. Er du sikker på at du vil fortsette?
        </BodyShort>
      </Modal.Body>
      <Modal.Footer>
        <HStack gap="space-16">
          <Button type="button" variant="secondary" onClick={onClose}>
            Nei, takk
          </Button>
          <Button
            data-color="danger"
            title="Slett utbetaling"
            variant="primary"
            onClick={slettKorreksjon}
          >
            Ja, jeg vil slette utbetalingen
          </Button>
        </HStack>
      </Modal.Footer>
    </Modal>
  );
}
