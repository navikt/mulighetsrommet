import React from 'react';
import './Ikonknapp.less';
import { Button } from '@navikt/ds-react';
import classNames from 'classnames';

interface SidemenyKnappProps {
  children: React.ReactNode;
  className?: string;
  handleClick: () => void;
  ariaLabel: string;
}

const Ikonknapp = ({ children, ariaLabel, className, handleClick }: SidemenyKnappProps) => {
  return (
    <Button
      onClick={handleClick}
      variant="tertiary"
      className={classNames('ikonknapp', className)}
      aria-label={ariaLabel}
    >
      {children}
    </Button>
  );
};

export default Ikonknapp;
