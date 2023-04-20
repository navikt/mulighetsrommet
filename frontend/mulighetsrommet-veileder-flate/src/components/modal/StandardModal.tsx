import { Button, Heading, Modal } from '@navikt/ds-react';
import React from 'react';
import classNames from 'classnames';
import styles from './Modal.module.scss';

interface StandardModalModalProps {
  modalOpen: boolean;
  heading: string;
  children: React.ReactNode;
  setModalOpen: () => void;
  handleForm?: () => void;
  handleCancel?: () => void;
  className?: string;
  btnText?: string;
  shouldCloseOnOverlayClick?: boolean;
  hideButtons?: boolean;
  id?: string;
}

const StandardModal = ({
  modalOpen,
  heading,
  children,
  setModalOpen,
  handleForm,
  handleCancel,
  className,
  btnText,
  shouldCloseOnOverlayClick,
  hideButtons = false,
  id,
}: StandardModalModalProps) => {
  const clickSend = () => {
    setModalOpen();
    handleForm?.();
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
      className={classNames(styles.overstyrte_styles_fra_ds_modal, className)}
      aria-label="modal"
    >
      <Modal.Content id={id || ''}>
        <Heading spacing level="1" size="medium" data-testid="modal_header">
          {heading}
        </Heading>
        {children}
        {!hideButtons ? (
          <div className={styles.modal_btngroup}>
            <Button onClick={clickSend} data-testid="modal_btn-send">
              {btnText || 'Send'}
            </Button>
            <Button variant="tertiary" onClick={clickCancel} data-testid="modal_btn-cancel">
              Avbryt
            </Button>
          </div>
        ) : null}
      </Modal.Content>
    </Modal>
  );
};

export default StandardModal;
