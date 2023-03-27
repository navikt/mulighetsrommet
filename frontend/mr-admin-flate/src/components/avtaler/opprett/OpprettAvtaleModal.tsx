import { Heading, Modal } from "@navikt/ds-react";
import React from "react";
import styles from "./Modal.module.scss";
import { OpprettAvtaleContainer } from "./OpprettAvtaleContainer";

interface OpprettAvtaleModalProps {
  modalOpen: boolean;
  onClose: () => void;
  handleForm?: () => void;
  handleCancel?: () => void;
  shouldCloseOnOverlayClick?: boolean;
}

const OpprettAvtaleModal = ({
  modalOpen,
  onClose,
  handleForm,
  handleCancel,
}: OpprettAvtaleModalProps) => {
  const clickSend = () => {
    handleForm?.();
  };

  const clickCancel = () => {
    onClose();
    handleCancel!();
  };

  return (
    <Modal
      shouldCloseOnOverlayClick={false}
      closeButton
      open={modalOpen}
      onClose={onClose}
      className={styles.overstyrte_styles_fra_ds_modal}
      aria-label="modal"
    >
      <Modal.Content>
        <Heading size="small" level="2" data-testid="modal_header">
          Registrer ny avtale
        </Heading>
        <OpprettAvtaleContainer />
      </Modal.Content>
    </Modal>
  );
};

export default OpprettAvtaleModal;
