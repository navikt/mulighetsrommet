import { BodyShort, Button, Heading, Modal } from "@navikt/ds-react";
import { RefObject } from "react";
import styles from "./EndreDatoAdvarselModal.module.scss";

interface Props {
  modalRef: RefObject<HTMLDialogElement>;
  onCancel: () => void;
  antallDeltakere: number;
}

export function EndreDatoAdvarselModal({ modalRef, onCancel, antallDeltakere }: Props) {
  const onClose = () => {
    modalRef.current?.close();
  };

  return (
    <Modal ref={modalRef} onClose={onClose} closeOnBackdropClick aria-label="modal">
      <Modal.Header closeButton={false}>
        <div className={styles.heading}>
          <Heading size="medium">Det finnes brukere påmeldt denne gjennomføringen</Heading>
        </div>
      </Modal.Header>
      <Modal.Body>
        <BodyShort>
          {`Det finnes ${antallDeltakere} deltaker${antallDeltakere > 1 ? "e" : ""}
            på gjennomføringen. Ved å endre dato for gjennomføringen kan det medføre
            at datoer for deltakerne også oppdateres automatisk.`}
        </BodyShort>
      </Modal.Body>
      <Modal.Footer className={styles.footer}>
        <Button
          type="button"
          variant="tertiary"
          onClick={() => {
            onCancel();
            onClose();
          }}
        >
          Nei, takk
        </Button>
        <Button type="button" variant="primary" onClick={onClose}>
          Ja, jeg vil endre dato
        </Button>
      </Modal.Footer>
    </Modal>
  );
}
