import React from 'react';
import './Deleknapp.less';
import { Button } from '@navikt/ds-react';
import classNames from 'classnames';

interface DeleknappProps {
  children: React.ReactNode;
  className?: string;
  handleClick: () => void;
  ariaLabel: string;
  dataTestId?: string;
  disabled?: boolean;
}

const Deleknapp = ({ children, ariaLabel, className, handleClick, dataTestId, disabled = false }: DeleknappProps) => {
  return (
    <Button
      onClick={handleClick}
      variant="secondary"
      className={classNames('deleknapp', className)}
      aria-label={ariaLabel}
      data-testid={dataTestId}
      disabled={disabled}
    >
      {children}
    </Button>
  );
};

export default Deleknapp;
