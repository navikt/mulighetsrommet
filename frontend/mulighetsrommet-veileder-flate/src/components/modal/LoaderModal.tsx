import { Loader, Modal } from '@navikt/ds-react';

interface LoaderModalProps {
  lukkModal: () => void;
  modalOpen: boolean;
}

export const LoaderModal = ({ lukkModal, modalOpen }: LoaderModalProps) => {
  return (
    <Modal onClose={lukkModal} open={modalOpen}>
      <Modal.Header closeButton={false} />
      <Modal.Body>
        <Loader size="xlarge" />
      </Modal.Body>
    </Modal>
  );
};
