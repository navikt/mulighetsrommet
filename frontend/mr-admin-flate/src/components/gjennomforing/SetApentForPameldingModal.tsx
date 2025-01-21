import { useSetApentForPamelding } from "@/api/gjennomforing/useSetApentForPamelding";
import { GjennomforingDto } from "@mr/api-client-v2";
import { Alert, Button, Modal, Switch } from "@navikt/ds-react";
import { RefObject } from "react";
import { useRevalidator } from "react-router";

interface Props {
  modalRef: RefObject<HTMLDialogElement>;
  gjennomforing: GjennomforingDto;
}

export function SetApentForPameldingModal({ modalRef, gjennomforing }: Props) {
  const { mutate } = useSetApentForPamelding(gjennomforing.id);
  const revalidator = useRevalidator();

  return (
    <Modal ref={modalRef} header={{ heading: "Åpent for påmelding" }}>
      <Modal.Body className="prose">
        <div>
          <p>Her kan du styre om tiltaket skal være åpent for påmelding i Modia.</p>

          <Alert variant={"info"}>
            For tiltak hvor påmelding fortsatt gjøres i Arena, vil det være mulig å melde på
            deltakere selv om tiltaket vises som stengt i Modia.
          </Alert>

          <p>Påmelding stenges automatisk av systemet når:</p>
          <ul>
            <li>
              Tiltak med <b>felles oppstart</b> starter.
            </li>
            <li>Tiltaket avsluttes eller blir avbrutt.</li>
          </ul>

          <Switch
            checked={gjennomforing.apentForPamelding}
            onChange={(e) => mutate(e.target.checked, { onSuccess: revalidator.revalidate })}
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
