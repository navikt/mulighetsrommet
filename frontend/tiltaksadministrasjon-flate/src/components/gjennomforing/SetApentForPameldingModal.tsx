import { useSetApentForPamelding } from "@/api/gjennomforing/useSetApentForPamelding";
import { BodyShort, Button, List, Modal, Switch } from "@navikt/ds-react";
import { RefObject } from "react";

interface Props {
  modalRef: RefObject<HTMLDialogElement | null>;
  gjennomforingId: string;
  apentForPamelding: boolean;
}

export function SetApentForPameldingModal({ modalRef, gjennomforingId, apentForPamelding }: Props) {
  const setApentForPamelding = useSetApentForPamelding(gjennomforingId);

  return (
    <Modal ref={modalRef} header={{ heading: "Åpent for påmelding" }} width={1000}>
      <Modal.Body className="prose">
        <div>
          <BodyShort spacing>
            Her kan du styre om tiltaket skal være åpent for påmelding i Modia.
          </BodyShort>

          <BodyShort>Påmelding stenges automatisk av systemet når:</BodyShort>

          <List>
            <List.Item>
              Tiltak med <b>felles oppstart</b> starter.
            </List.Item>
            <List.Item>Tiltaket avsluttes eller blir avbrutt.</List.Item>
          </List>

          <Switch
            checked={apentForPamelding}
            onChange={(e) => setApentForPamelding.mutate(e.target.checked)}
          >
            Åpent for påmelding
          </Switch>
        </div>
      </Modal.Body>
      <Modal.Footer>
        <Button type="button" onClick={() => modalRef.current?.close()}>
          Lukk
        </Button>
      </Modal.Footer>
    </Modal>
  );
}
