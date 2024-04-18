import { ExclamationmarkTriangleIcon } from "@navikt/aksel-icons";
import { BodyShort, Button, Heading, Modal } from "@navikt/ds-react";
import { RefObject } from "react";
import styles from "./StartDatoAdvarselModal.module.scss";

interface Props {
  modalRef: RefObject<HTMLDialogElement>;
  onCancel: () => void;
  antallDeltakere: number;
}

export function StartDatoAdvarselModal({ modalRef, onCancel, antallDeltakere }: Props) {
  const onClose = () => {
    modalRef.current?.close();
  };

  return (
    <Modal ref={modalRef} onClose={onClose} closeOnBackdropClick aria-label="modal">
      <Modal.Header closeButton={false}>
        <div className={styles.heading}>
          <Heading size="medium">
            <ExclamationmarkTriangleIcon className={styles.warning_icon} />
            Det finnes brukere påmeldt denne gjennomføringen
          </Heading>
        </div>
      </Modal.Header>
      <Modal.Body>
        <BodyShort>
          {`Det finnes ${antallDeltakere} deltaker${antallDeltakere > 1 ? "e" : ""}
            på gjennomføringen. Ved å utsette gjennomføringen vil det føre til statusendring
            på alle deltakere som har en aktiv status.`}
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
          Ja, jeg vil utsette gjennomføringen
        </Button>
      </Modal.Footer>
    </Modal>
  );
}
