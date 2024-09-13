import { Button, Heading, Modal } from "@navikt/ds-react";
import classNames from "classnames";
import React, { RefObject } from "react";
import styles from "./VarselModal.module.scss";
import {
  ExclamationmarkTriangleFillIcon,
  InformationSquareFillIcon,
  XMarkOctagonFillIcon,
} from "@navikt/aksel-icons";

interface Props {
  modalRef: RefObject<HTMLDialogElement>;
  open?: boolean;
  handleClose: () => void;
  headingIconType?: "warning" | "error" | "info";
  headingText: React.ReactNode;
  body: React.ReactNode;
  primaryButton?: React.ReactNode;
  secondaryButton?: boolean;
  secondaryButtonHandleAction?: () => void;
  footerClassName?: string;
}

export function VarselModal({
  modalRef,
  open,
  handleClose,
  headingIconType,
  headingText,
  body,
  primaryButton,
  secondaryButton = false,
  secondaryButtonHandleAction,
  footerClassName,
}: Props) {
  return (
    <Modal
      ref={modalRef}
      onClose={handleClose}
      closeOnBackdropClick
      aria-label="modal"
      open={open}
      className={styles.varselmodal}
    >
      <Modal.Header closeButton={false} className={styles.heading}>
        {headingIconType === "warning" && (
          <ExclamationmarkTriangleFillIcon
            className={classNames(styles.icon, styles.icon_warning)}
          />
        )}
        {headingIconType === "error" && (
          <XMarkOctagonFillIcon className={classNames(styles.icon, styles.icon_error)} />
        )}
        {headingIconType === "info" && (
          <InformationSquareFillIcon className={classNames(styles.icon, styles.icon_info)} />
        )}
        <Heading align="center" size="medium">
          {headingText}
        </Heading>
      </Modal.Header>
      <Modal.Body className={styles.body}>{body}</Modal.Body>
      <Modal.Footer className={footerClassName ? footerClassName : styles.footer}>
        {secondaryButton && (
          <Button
            variant="secondary"
            onClick={() => {
              handleClose();
              secondaryButtonHandleAction?.();
            }}
          >
            Nei, takk
          </Button>
        )}
        {primaryButton} {/*Teksten i primarybutton skal være "Ja, jeg vil .."*/}
      </Modal.Footer>
    </Modal>
  );
}
