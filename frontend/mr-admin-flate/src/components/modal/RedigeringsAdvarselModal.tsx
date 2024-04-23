import styles from "./Modal.module.scss";
import { BodyShort, Button, Heading, Modal } from "@navikt/ds-react";
import { RefObject } from "react";

interface Props {
  modalRef: RefObject<HTMLDialogElement>;
  onRediger: () => void;
  ressursNavn: string;
}

export function RedigeringsAdvarselModal({ modalRef, onRediger, ressursNavn }: Props) {
  const onClose = () => {
    modalRef.current?.close();
  };

  return (
    <Modal ref={modalRef} onClose={onClose} closeOnBackdropClick aria-label="modal">
      <Modal.Header closeButton={false}>
        <div className={styles.heading}>
          <Heading size="medium">{`Du er ikke eier av denne ${ressursNavn}`}</Heading>
        </div>
      </Modal.Header>
      <Modal.Body>
        <BodyShort>Vil du fortsette til redigeringen?</BodyShort>
      </Modal.Body>
      <Modal.Footer>
        <div className={styles.knapperad}>
          <Button variant="secondary" type="button" onClick={onClose}>
            Nei, takk
          </Button>
          <Button variant="primary" onClick={onRediger}>
            Ja, jeg vil redigere
          </Button>
        </div>
      </Modal.Footer>
    </Modal>
  );
}
