import { useSettPaVentGjennomforingOkonomi } from "@/api/gjennomforing/useSettPaVentGjennomforingOkonomi";
import { BodyShort, Button, Modal, Textarea } from "@navikt/ds-react";
import { useState } from "react";

interface Props {
  open: boolean;
  setOpen: (open: boolean) => void;
  gjennomforingId: string;
}

export function SettPaVentOkonomiModal({ open, setOpen, gjennomforingId }: Props) {
  const settPaVentMutation = useSettPaVentGjennomforingOkonomi();
  const [forklaring, setForklaring] = useState("");

  function close() {
    setOpen(false);
    setForklaring("");
  }

  function settPaVent() {
    settPaVentMutation.mutate(
      { id: gjennomforingId, forklaring: forklaring || null },
      { onSuccess: close },
    );
  }

  return (
    <Modal open={open} onClose={close} header={{ heading: "Sett på vent" }} width="medium">
      <Modal.Body>
        <BodyShort spacing>
          Du er i ferd med å sette besluttelse av økonomi for gjennomføringen på vent. Du kan
          eventuelt legge inn en forklaring nedenfor.
        </BodyShort>
        <Textarea
          label="Forklaring (valgfritt)"
          value={forklaring}
          onChange={(e) => setForklaring(e.target.value)}
          maxLength={500}
        />
      </Modal.Body>
      <Modal.Footer>
        <Button variant="primary" onClick={settPaVent} loading={settPaVentMutation.isPending}>
          Sett på vent
        </Button>
        <Button variant="secondary" onClick={close}>
          Avbryt
        </Button>
      </Modal.Footer>
    </Modal>
  );
}
