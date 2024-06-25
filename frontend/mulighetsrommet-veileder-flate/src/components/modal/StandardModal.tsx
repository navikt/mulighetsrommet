import { Button, Heading, Modal } from "@navikt/ds-react";
import React from "react";
import styles from "./Modal.module.scss";

interface StandardModalModalProps {
  modalOpen: boolean;
  heading: string | React.ReactNode;
  children: React.ReactNode;
  setModalOpen: () => void;
  handlePrimaryButton?: () => void;
  handleCancel?: () => void;
  className?: string;
  btnText?: string;
  hideButtons?: boolean;
  id?: string;
  closeButton?: boolean;
}

const StandardModal = ({
  modalOpen,
  heading,
  children,
  setModalOpen,
  handlePrimaryButton,
  handleCancel,
  className,
  btnText,
  hideButtons = false,
  id,
  closeButton = true,
}: StandardModalModalProps) => {
  const clickPrimaryButton = () => {
    setModalOpen();
    handlePrimaryButton?.();
  };

  const clickCancel = () => {
    setModalOpen();
    handleCancel!();
  };

  return (
    <Modal
      open={modalOpen}
      onClose={setModalOpen}
      className={className}
      aria-label="modal"
      id={id}
      data-testid={id}
      closeOnBackdropClick
    >
      <Modal.Header closeButton={closeButton}>
        <Heading spacing size="medium">
          {heading}
        </Heading>
      </Modal.Header>
      <Modal.Body>{children}</Modal.Body>
      {!hideButtons ? (
        <Modal.Footer>
          <div className={styles.knapperad}>
            <Button variant="tertiary" onClick={clickCancel} data-testid="modal_btn-cancel">
              Avbryt
            </Button>
            <Button onClick={clickPrimaryButton}>{btnText}</Button>
          </div>
        </Modal.Footer>
      ) : null}
    </Modal>
  );
};

export default StandardModal;
