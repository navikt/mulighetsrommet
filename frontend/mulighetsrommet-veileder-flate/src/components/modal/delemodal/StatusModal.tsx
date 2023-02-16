import { ErrorColored, SuccessColored, WarningColored } from '@navikt/ds-icons';
<<<<<<< HEAD
import { BodyShort, Button, Heading, Modal } from '@navikt/ds-react';
import style from './Statusmodal.module.scss';
import modalStyles from '../Modal.module.scss';

interface StatusModalProps {
  modalOpen: boolean;
  onClose: () => void;
=======
import { BodyShort, Button, Heading } from '@navikt/ds-react';
import style from './Statusmodal.module.scss';

interface StatusModalProps {
>>>>>>> a82247d1 (merge)
  ikonVariant: string;
  heading?: string | null;
  text: string;
  primaryButtonText: string | React.ReactNode;
  primaryButtonOnClick: () => void;
  secondaryButtonText?: string | null;
  secondaryButtonOnClick?: () => void;
}

export function StatusModal({
<<<<<<< HEAD
  modalOpen,
  onClose,
=======
>>>>>>> a82247d1 (merge)
  ikonVariant,
  heading,
  text,
  primaryButtonText,
  primaryButtonOnClick,
  secondaryButtonText,
  secondaryButtonOnClick,
}: StatusModalProps) {
  const ikon = () => {
    if (ikonVariant === 'success') return <SuccessColored className={style.svg} />;
    else if (ikonVariant === 'warning') return <WarningColored className={style.svg} />;
    else if (ikonVariant === 'error') return <ErrorColored className={style.svg} />;
  };

  return (
<<<<<<< HEAD
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
=======
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
>>>>>>> a82247d1 (merge)
  );
}
