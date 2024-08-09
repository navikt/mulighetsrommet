import { BodyShort, Button, Heading, Modal } from "@navikt/ds-react";
import modalStyles from "./Modal.module.scss";
import svgStyle from "../../App.module.scss";

import React from "react";
import {
  CheckmarkCircleFillIcon,
  ExclamationmarkTriangleFillIcon,
  XMarkOctagonFillIcon,
} from "@navikt/aksel-icons";
import classNames from "classnames";

interface StatusModalProps {
  modalOpen: boolean;
  onClose: () => void;
  ikonVariant: string;
  heading?: string | null;
  text: string | React.ReactNode;
  primaryButtonText: string | React.ReactNode;
  primaryButtonOnClick: (event: any) => void;
  secondaryButtonText?: string | null;
  secondaryButtonOnClick?: () => void;
}

export function StatusModal({
  modalOpen,
  onClose,
  ikonVariant,
  heading,
  text,
  primaryButtonText,
  primaryButtonOnClick,
  secondaryButtonText,
  secondaryButtonOnClick,
}: StatusModalProps) {
  const ikon = () => {
    if (ikonVariant === "success")
      return (
        <CheckmarkCircleFillIcon className={classNames(modalStyles.icon, svgStyle.svg_success)} />
      );
    else if (ikonVariant === "warning")
      return (
        <ExclamationmarkTriangleFillIcon
          className={classNames(modalStyles.icon, svgStyle.svg_warning)}
        />
      );
    else if (ikonVariant === "error")
      return <XMarkOctagonFillIcon className={classNames(modalStyles.icon, svgStyle.svg_error)} />;
  };

  function footerInnhold() {
    return (
      <div className={modalStyles.knapperad}>
        {secondaryButtonText ? (
          <Button
            variant="secondary"
            onClick={secondaryButtonOnClick}
            data-testid="modal_btn-cancel"
          >
            {secondaryButtonText}
          </Button>
        ) : null}
        <Button variant="primary" onClick={(event) => primaryButtonOnClick(event)}>
          {primaryButtonText}
        </Button>
      </div>
    );
  }

  return (
    <Modal
      open={modalOpen}
      onClose={onClose}
      data-testid="statusmodal"
      aria-label={heading || "Statusmodal"}
    >
      <Modal.Header closeButton={false}>
        <Heading align="center" size="medium" className={modalStyles.heading}>
          {ikon()}
          {heading}
        </Heading>
      </Modal.Header>
      <Modal.Body>
        <BodyShort align="center">{text}</BodyShort>
      </Modal.Body>
      <Modal.Footer>{footerInnhold()}</Modal.Footer>
    </Modal>
  );
}
