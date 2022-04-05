import { Button, Heading, Modal } from '@navikt/ds-react';
import React from 'react';
import './modal.less';
import classNames from 'classnames';

interface SendInformasjonModalProps {
  modalOpen: boolean;
  setModalOpen: () => void;
  handleForm?: () => void;
  heading: string;
  className?: string;
  btnText?: string;
  children: React.ReactNode;
}

const StandardModal = ({
  modalOpen,
  setModalOpen,
  handleForm,
  heading,
  className,
  btnText,
  children,
}: SendInformasjonModalProps) => {
  const handleSend = () => {
    handleForm!();
    setModalOpen();
  };

  return (
    <Modal
      closeButton
      open={modalOpen}
      onClose={setModalOpen}
      className={classNames('mulighetsrommet-veileder-flate__modal', className)}
      aria-label="modal"
    >
      <Modal.Content>
        <Heading spacing level="1" size="large" data-testid="modal_header">
          {heading}
        </Heading>
        {children}
        <div className="modal_btngroup">
          <Button onClick={handleSend} data-testid="modal_btn-send">
            {btnText || 'Send'}
          </Button>
          <Button variant="tertiary" onClick={setModalOpen} data-testid="modal_btn-cancel">
            Avbryt
          </Button>
        </div>
      </Modal.Content>
    </Modal>
  );
};

export default StandardModal;
