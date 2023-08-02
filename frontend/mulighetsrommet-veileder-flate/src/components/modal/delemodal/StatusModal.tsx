import { BodyShort, Button, Heading, Modal } from '@navikt/ds-react';
import style from './Statusmodal.module.scss';
import modalStyles from '../Modal.module.scss';
import svgStyle from '../../../App.module.scss';

import React from 'react';
import { CheckmarkCircleFillIcon, ExclamationmarkTriangleFillIcon, XMarkOctagonFillIcon } from '@navikt/aksel-icons';

interface StatusModalProps {
  modalOpen: boolean;
  onClose: () => void;
  ikonVariant: string;
  heading?: string | null;
  text: React.ReactNode;
  primaryButtonText: string | React.ReactNode;
  primaryButtonOnClick: () => void;
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
    if (ikonVariant === 'success') return <CheckmarkCircleFillIcon className={svgStyle.svg_success} />;
    else if (ikonVariant === 'warning') return <ExclamationmarkTriangleFillIcon className={svgStyle.svg_warning} />;
    else if (ikonVariant === 'error') return <XMarkOctagonFillIcon className={svgStyle.svg_error} />;
  };

  return (
    <Modal
      open={modalOpen}
      onClose={onClose}
      closeButton={false}
      shouldCloseOnOverlayClick={true}
      className={modalStyles.overstyrte_styles_fra_ds_modal}
    >
      <Modal.Content>
        <div className={style.container}>
          {ikon()}
          {heading && (
            <Heading level="1" size="medium" data-testid="modal_header">
              {heading}
            </Heading>
          )}
          <BodyShort className={style.text}>{text}</BodyShort>
          <div className={style.modal_btngroup}>
            <Button variant="primary" onClick={primaryButtonOnClick}>
              {primaryButtonText}
            </Button>
            {secondaryButtonText && (
              <Button variant="secondary" onClick={secondaryButtonOnClick} data-testid="modal_btn-cancel">
                {secondaryButtonText}
              </Button>
            )}
          </div>
        </div>
      </Modal.Content>
    </Modal>
  );
}
