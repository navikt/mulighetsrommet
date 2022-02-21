import React from 'react';
import './Ikonknapp.less';
import { Button } from '@navikt/ds-react';
import classNames from 'classnames';

interface SidemenyKnappProps {
  children: React.ReactNode;
  className?: string;
  handleClick: () => void;
}

const Ikonknapp = ({ children, className, handleClick }: SidemenyKnappProps) => {
  return (
    <Button onClick={handleClick} variant="tertiary" className={classNames('ikonknapp', className)}>
      {children}
    </Button>
  );
};

export default Ikonknapp;
