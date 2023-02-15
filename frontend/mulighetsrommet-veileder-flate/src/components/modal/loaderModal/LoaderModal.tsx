import { Loader, Modal } from '@navikt/ds-react';

interface LoaderModalProps {
  lukkModal: () => void;
  modalOpen: boolean;
}

export const LoaderModal = ({ lukkModal, modalOpen }: LoaderModalProps) => {
  return (
    <Modal onClose={lukkModal} open={modalOpen} closeButton={false}>
      <Modal.Content>
        <Loader size="xlarge" />
      </Modal.Content>
    </Modal>
  );
};
