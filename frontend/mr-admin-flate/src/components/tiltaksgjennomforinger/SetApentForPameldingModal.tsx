import { Alert, BodyLong, Button, Modal, Switch } from "@navikt/ds-react";
import { RefObject } from "react";
import { TiltaksgjennomforingDto } from "@mr/api-client";
import { useSetApentForPamelding } from "@/api/tiltaksgjennomforing/useSetApentForPamelding";

interface Props {
  modalRef: RefObject<HTMLDialogElement>;
  gjennomforing: TiltaksgjennomforingDto;
}

export function SetApentForPameldingModal({ modalRef, gjennomforing }: Props) {
  const { mutate } = useSetApentForPamelding(gjennomforing.id);

  return (
    <Modal ref={modalRef} header={{ heading: "Åpent for påmelding" }}>
      <Modal.Body>
        <BodyLong>
          <p>Her kan du styre om tiltaket skal være åpent for påmelding i Modia.</p>

          <Alert variant={"info"}>
            For tiltak hvor påmelding fortsatt gjøres i Arena, vil det være mulig å melde på
            deltakere selv om tiltaket vises som stengt i Modia.
          </Alert>

          <p>
            Påmelding stenges automatisk av systemet når:
            <ul>
              <li>
                Tiltak med <b>felles oppstart</b> starter.
              </li>
              <li>Tiltaket avsluttes eller blir avbrutt.</li>
            </ul>
          </p>

          <Switch
            checked={gjennomforing.apentForPamelding}
            onChange={(e) => mutate(e.target.checked)}
          >
            Åpent for påmelding
          </Switch>
        </BodyLong>
      </Modal.Body>
      <Modal.Footer>
        <Button type="button" onClick={() => modalRef.current?.close()}>
          Lukk
        </Button>
      </Modal.Footer>
    </Modal>
  );
}