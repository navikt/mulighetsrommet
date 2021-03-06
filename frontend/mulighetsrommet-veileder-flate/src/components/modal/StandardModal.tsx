import { Button, Heading, Modal } from '@navikt/ds-react';
import React from 'react';
import './modal.less';
import classNames from 'classnames';

interface StandardModalModalProps {
  modalOpen: boolean;
  setModalOpen: () => void;
  handleForm: () => void;
  handleCancel?: () => void;
  heading: string;
  className?: string;
  btnText?: string;
  children: React.ReactNode;
  shouldCloseOnOverlayClick?: boolean;
}

const StandardModal = ({
  modalOpen,
  setModalOpen,
  handleForm,
  handleCancel,
  heading,
  className,
  btnText,
  children,
  shouldCloseOnOverlayClick,
}: StandardModalModalProps) => {
  const clickSend = () => {
    setModalOpen();
    handleForm();
  };

  const clickCancel = () => {
    setModalOpen();
    handleCancel!();
  };

  return (
    <Modal
      shouldCloseOnOverlayClick={shouldCloseOnOverlayClick}
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
          <Button onClick={clickSend} data-testid="modal_btn-send">
            {btnText || 'Send'}
          </Button>
          <Button variant="tertiary" onClick={clickCancel} data-testid="modal_btn-cancel">
            Avbryt
          </Button>
        </div>
      </Modal.Content>
    </Modal>
  );
};

export default StandardModal;
