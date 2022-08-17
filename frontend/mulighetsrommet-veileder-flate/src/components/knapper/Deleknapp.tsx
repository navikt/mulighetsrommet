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
}

const Deleknapp = ({ children, ariaLabel, className, handleClick, dataTestId }: DeleknappProps) => {
  return (
    <Button
      onClick={handleClick}
      variant="tertiary"
      className={classNames('deleknapp', className)}
      aria-label={ariaLabel}
      data-testid={dataTestId}
    >
      {children}
    </Button>
  );
};

export default Deleknapp;
