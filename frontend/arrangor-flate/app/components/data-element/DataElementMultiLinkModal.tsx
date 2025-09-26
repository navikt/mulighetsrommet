import { DataElementMultiLinkModalModalContent, type DataElementMultiLinkModal } from "@api-client";
import { BodyLong, Button, Link, Modal } from "@navikt/ds-react";
import { RefObject, useRef } from "react";

interface DataElementMultiLinkModalProps {
  data: DataElementMultiLinkModal;
}

export function DataElementMultiLinkModal({ data }: DataElementMultiLinkModalProps) {
  const modalRef = useRef<HTMLDialogElement>(null);

  return (
    <>
      <Button size="small" variant="secondary" onClick={() => modalRef.current?.showModal()}>
        {data.buttonText}
      </Button>
      <MultiLinkModal modalRef={modalRef} modalContent={data.modalContent} />
    </>
  );
}

interface MultiLinkModalProps {
  modalRef: RefObject<HTMLDialogElement | null>;
  modalContent: DataElementMultiLinkModalModalContent;
}

function MultiLinkModal({ modalRef, modalContent }: MultiLinkModalProps) {
  return (
    <Modal
      ref={modalRef}
      header={{ heading: modalContent.header }}
      onClose={() => modalRef.current?.close()}
      closeOnBackdropClick
    >
      <Modal.Body>
        <BodyLong>{modalContent.description}</BodyLong>
      </Modal.Body>
      <Modal.Footer>
        {modalContent.links.map((lenke) => (
          <Button key={lenke.digest} as={Link} href={lenke.href} variant="secondary">
            {lenke.text}
          </Button>
        ))}
      </Modal.Footer>
    </Modal>
  );
}
