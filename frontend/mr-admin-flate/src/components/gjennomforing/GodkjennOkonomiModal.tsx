import { useGodkjennGjennomforingOkonomi } from "@/api/gjennomforing/useGodkjennGjennomforingOkonomi";
import { BodyShort, Button, Modal } from "@navikt/ds-react";

interface Props {
  open: boolean;
  setOpen: (open: boolean) => void;
  gjennomforingId: string;
}

export function GodkjennOkonomiModal({ open, setOpen, gjennomforingId }: Props) {
  const godkjennMutation = useGodkjennGjennomforingOkonomi();

  function close() {
    setOpen(false);
  }

  function godkjenn() {
    godkjennMutation.mutate({ id: gjennomforingId }, { onSuccess: close });
  }

  return (
    <Modal open={open} onClose={close} header={{ heading: "Godkjenn enkeltplass" }} width="medium">
      <Modal.Body>
        <BodyShort>
          Bekreft at du har gjennomgått og godkjenner økonomien for gjennomføringen.
        </BodyShort>
      </Modal.Body>
      <Modal.Footer>
        <Button onClick={godkjenn} loading={godkjennMutation.isPending}>
          Godkjenn enkeltplass
        </Button>
        <Button variant="secondary" onClick={close}>
          Avbryt
        </Button>
      </Modal.Footer>
    </Modal>
  );
}
