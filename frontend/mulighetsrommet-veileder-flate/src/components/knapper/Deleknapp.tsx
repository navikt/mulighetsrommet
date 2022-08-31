import React from 'react';
import './Deleknapp.less';
import { Button } from '@navikt/ds-react';
import classNames from 'classnames';
import { DELING_MED_BRUKER, useFeatureToggles } from '../../core/api/feature-toggles';

interface DeleknappProps {
  children: React.ReactNode;
  className?: string;
  handleClick: () => void;
  ariaLabel: string;
  dataTestId?: string;
  disabled?: boolean;
}

const Deleknapp = ({ children, ariaLabel, className, handleClick, dataTestId, disabled = false }: DeleknappProps) => {
  const features = useFeatureToggles();
  const visDeleknapp = features.isSuccess && features.data[DELING_MED_BRUKER];

  return (
    <>
      {visDeleknapp && (
        <Button
          onClick={handleClick}
          variant="tertiary"
          className={classNames('deleknapp', className)}
          aria-label={ariaLabel}
          data-testid={dataTestId}
          disabled={disabled}
        >
          {children}
        </Button>
      )}
    </>
  );
};

export default Deleknapp;
