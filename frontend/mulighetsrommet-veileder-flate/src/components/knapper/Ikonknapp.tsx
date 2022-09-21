import React from 'react';
import './Ikonknapp.less';
import { Button } from '@navikt/ds-react';
import classNames from 'classnames';

interface SidemenyKnappProps {
  children?: React.ReactNode;
  icon?: React.ReactNode;
  className?: string;
  handleClick: () => void;
  ariaLabel: string;
  dataTestId?: string;
}

const Ikonknapp = ({ children, icon, ariaLabel, className, handleClick, dataTestId }: SidemenyKnappProps) => {
  return (
    <Button
      onClick={handleClick}
      icon={icon}
      variant="tertiary"
      className={classNames('ikonknapp', className)}
      aria-label={ariaLabel}
      data-testid={dataTestId}
    >
      {children}
    </Button>
  );
};

export default Ikonknapp;
