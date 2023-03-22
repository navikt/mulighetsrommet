import { Button, Heading, Modal } from "@navikt/ds-react";
import React from "react";
import classNames from "classnames";
import styles from "./Modal.module.scss";
import { OpprettAvtaleContainer } from "./OpprettAvtaleContainer";

interface OpprettAvtaleModalProps {
  modalOpen: boolean;
  setModalOpen: () => void;
  handleForm?: () => void;
  handleCancel?: () => void;
  className?: string;
  shouldCloseOnOverlayClick?: boolean;
}

const OpprettAvtaleModal = ({
  modalOpen,
  setModalOpen,
  handleForm,
  handleCancel,
  className,
}: OpprettAvtaleModalProps) => {
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
      shouldCloseOnOverlayClick={false}
      closeButton
      open={modalOpen}
      onClose={setModalOpen}
      className={classNames(styles.overstyrte_styles_fra_ds_modal, className)}
      aria-label="modal"
    >
      <Modal.Content>
        <OpprettAvtaleContainer />
      </Modal.Content>
    </Modal>
  );
};

export default OpprettAvtaleModal;
