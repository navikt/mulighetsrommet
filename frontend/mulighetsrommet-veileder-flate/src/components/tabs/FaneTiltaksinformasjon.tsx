import classNames from 'classnames';
import React, { ReactNode } from 'react';
import styles from './Detaljerfane.module.scss';

interface FaneMalTiltakProps {
  children: ReactNode;
  harInnhold: boolean;
  className?: string;
}

const FaneTiltaksinformasjon = ({ children, harInnhold, className }: FaneMalTiltakProps) => {
  return <div className={classNames(styles.tiltaksdetaljer_maksbredde, className)}>{harInnhold ? children : null}</div>;
};

export default FaneTiltaksinformasjon;
