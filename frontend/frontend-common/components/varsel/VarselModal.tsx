import { Button, Modal } from "@navikt/ds-react";
import React, { RefObject } from "react";
import {
  ExclamationmarkTriangleFillIcon,
  InformationSquareFillIcon,
  XMarkOctagonFillIcon,
} from "@navikt/aksel-icons";

interface Props {
  modalRef?: RefObject<HTMLDialogElement | null>;
  open?: boolean;
  handleClose: () => void;
  headingIconType?: "warning" | "error" | "info";
  headingText: string;
  body: React.ReactNode;
  primaryButton?: React.ReactNode;
  secondaryButton?: boolean;
  secondaryButtonHandleAction?: () => void;
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
}: Props) {
  const icon = () => {
    switch (headingIconType) {
      case "warning":
        return (
          <ExclamationmarkTriangleFillIcon
            color="var(--ax-text-warning-decoration)"
            height="1.5rem"
            width="1.5rem"
          />
        );
      case "error":
        return (
          <XMarkOctagonFillIcon
            color="var(--ax-text-error-decoration)"
            height="1.5rem"
            width="1.5rem"
          />
        );
      case "info":
        return (
          <InformationSquareFillIcon
            color="var(--ax-text-accent-decoration)"
            height="1.5rem"
            width="1.5rem"
          />
        );
      case undefined:
      default:
        return null;
    }
  };

  return (
    <Modal
      ref={modalRef}
      onClose={handleClose}
      header={{ heading: headingText, icon: icon() }}
      closeOnBackdropClick
      width="medium"
      open={open}
    >
      <Modal.Body>{body}</Modal.Body>
      <Modal.Footer>
        {primaryButton}
        {secondaryButton && (
          <Button
            type="button"
            variant="secondary"
            onClick={() => {
              handleClose();
              secondaryButtonHandleAction?.();
            }}
          >
            Avbryt
          </Button>
        )}
      </Modal.Footer>
    </Modal>
  );
}
