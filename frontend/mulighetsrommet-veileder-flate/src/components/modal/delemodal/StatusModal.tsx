import { ErrorColored, SuccessColored, WarningColored } from '@navikt/ds-icons';
import { BodyShort, Button, Heading } from '@navikt/ds-react';
import style from './Statusmodal.module.scss';

interface StatusModalProps {
  ikonVariant: string;
  heading?: string | null;
  text: string;
  primaryButtonText: string | React.ReactNode;
  primaryButtonOnClick: () => void;
  secondaryButtonText?: string | null;
  secondaryButtonOnClick?: () => void;
}

export function StatusModal({
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
  );
}
