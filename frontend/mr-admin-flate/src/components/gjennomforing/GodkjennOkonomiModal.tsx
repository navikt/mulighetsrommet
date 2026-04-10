import { useGodkjennGjennomforingOkonomi } from "@/api/gjennomforing/useGodkjennGjennomforingOkonomi";
import { useAvslaaGjennomforingOkonomi } from "@/api/gjennomforing/useAvslaaGjennomforingOkonomi";
import { BodyShort, Button, Modal, Textarea } from "@navikt/ds-react";
import { useState } from "react";

interface Props {
  open: boolean;
  setOpen: (open: boolean) => void;
  gjennomforingId: string;
}

export function GodkjennOkonomiModal({ open, setOpen, gjennomforingId }: Props) {
  const godkjennMutation = useGodkjennGjennomforingOkonomi();
  const avslaaMutation = useAvslaaGjennomforingOkonomi();

  const [avslaaMode, setAvslaaMode] = useState(false);
  const [forklaring, setForklaring] = useState("");

  function lukk() {
    setOpen(false);
    setAvslaaMode(false);
    setForklaring("");
  }

  function godkjenn() {
    godkjennMutation.mutate({ id: gjennomforingId }, { onSuccess: lukk });
  }

  function avsla() {
    avslaaMutation.mutate(
      { id: gjennomforingId, forklaring: forklaring || null },
      { onSuccess: lukk },
    );
  }

  return (
    <Modal open={open} onClose={lukk} header={{ heading: "Godkjenn økonomi" }} width="medium">
      <Modal.Body>
        {avslaaMode ? (
          <>
            <BodyShort spacing>
              Du er i ferd med å avslå økonomi for gjennomføringen. Du kan eventuelt legge inn en
              forklaring nedenfor.
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
            Bekreft at du har gjennomgått og godkjenner økonomien for gjennomføringen, eller avslå
            dersom det er feil.
          </BodyShort>
        )}
      </Modal.Body>
      <Modal.Footer>
        {avslaaMode ? (
          <>
            <Button variant="danger" onClick={avsla} loading={avslaaMutation.isPending}>
              Avslå
            </Button>
            <Button variant="secondary" onClick={() => setAvslaaMode(false)}>
              Tilbake
            </Button>
          </>
        ) : (
          <>
            <Button onClick={godkjenn} loading={godkjennMutation.isPending}>
              Bekreft
            </Button>
            <Button variant="secondary" onClick={() => setAvslaaMode(true)}>
              Avslå
            </Button>
          </>
        )}
      </Modal.Footer>
    </Modal>
  );
}
