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
          <p>Dette flagget styrer om deltakere kan meldes på fra Modia.</p>

          <Alert variant={"info"}>
            <b>Det er kun mulig å stenge for påmelding fra Modia og for AMO i Arena.</b> Hvis
            deltakelsen fortsatt administreres i Arena (og ikke er AMO) så vil ikke dette flagget ha
            noen effekt, men det vil likevel vises som stengt i Modia.
          </Alert>

          <p>
            Påmelding stenges automatisk av systemet når:
            <ul>
              <li>
                Tiltak med oppstartstype <b>felles oppstart</b> starter.
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
          Ferdig
        </Button>
      </Modal.Footer>
    </Modal>
  );
}
