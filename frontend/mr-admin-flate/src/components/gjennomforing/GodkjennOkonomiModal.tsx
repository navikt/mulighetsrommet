import { useGodkjennGjennomforingOkonomi } from "@/api/gjennomforing/useGodkjennGjennomforingOkonomi";
import { useSettPaVentGjennomforingOkonomi } from "@/api/gjennomforing/useSettPaVentGjennomforingOkonomi";
import { BodyShort, Button, Modal, Textarea } from "@navikt/ds-react";
import { useState } from "react";

interface Props {
  open: boolean;
  setOpen: (open: boolean) => void;
  gjennomforingId: string;
}

export function GodkjennOkonomiModal({ open, setOpen, gjennomforingId }: Props) {
  const godkjennMutation = useGodkjennGjennomforingOkonomi();
  const settPaVentMutation = useSettPaVentGjennomforingOkonomi();

  const [settPaVentMode, setSettPaVentMode] = useState(false);
  const [forklaring, setForklaring] = useState("");

  function lukk() {
    setOpen(false);
    setSettPaVentMode(false);
    setForklaring("");
  }

  function godkjenn() {
    godkjennMutation.mutate({ id: gjennomforingId }, { onSuccess: lukk });
  }

  function settPaVent() {
    settPaVentMutation.mutate(
      { id: gjennomforingId, forklaring: forklaring || null },
      { onSuccess: lukk },
    );
  }

  return (
    <Modal open={open} onClose={lukk} header={{ heading: "Godkjenn enkeltplass" }} width="medium">
      <Modal.Body>
        {settPaVentMode ? (
          <>
            <BodyShort spacing>
              Du er i ferd med å sette økonomi for gjennomføringen på vent. Du kan eventuelt legge
              inn en forklaring nedenfor.
            </BodyShort>
            <Textarea
              label="Forklaring (valgfritt)"
              value={forklaring}
              onChange={(e) => setForklaring(e.target.value)}
              maxLength={500}
            />
          </>
        ) : (
          <BodyShort>
            Bekreft at du har gjennomgått og godkjenner økonomien for gjennomføringen, eller sett på
            vent dersom flere avklaringer er nødvendig.
          </BodyShort>
        )}
      </Modal.Body>
      <Modal.Footer>
        {settPaVentMode ? (
          <>
            <Button variant="primary" onClick={settPaVent} loading={settPaVentMutation.isPending}>
              Sett på vent
            </Button>
            <Button variant="secondary" onClick={() => setSettPaVentMode(false)}>
              Tilbake
            </Button>
          </>
        ) : (
          <>
            <Button onClick={godkjenn} loading={godkjennMutation.isPending}>
              Bekreft
            </Button>
            <Button variant="secondary" onClick={() => setSettPaVentMode(true)}>
              Sett på vent
            </Button>
          </>
        )}
      </Modal.Footer>
    </Modal>
  );
}
