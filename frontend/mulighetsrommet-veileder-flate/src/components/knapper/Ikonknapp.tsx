import React from 'react';
import './Ikonknapp.less';
import { Button } from '@navikt/ds-react';
import classNames from 'classnames';
import { kebabCase } from '../../utils/Utils';

interface SidemenyKnappProps {
  children: React.ReactNode;
  className?: string;
  handleClick: () => void;
  ariaLabel: string;
  dataTestId?: string;
}

const Ikonknapp = ({ children, ariaLabel, className, handleClick, dataTestId }: SidemenyKnappProps) => {
  return (
    <Button
      onClick={handleClick}
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
