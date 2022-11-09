import classNames from 'classnames';
import React from 'react';
import styles from './Detaljerfane.module.scss';

interface FaneMalTiltakProps {
  children: any;
  harInnhold: boolean;
}

const FaneTiltaksinformasjon = ({ children, harInnhold }: FaneMalTiltakProps) => {
  return <div className={classNames(styles.tiltaksdetaljer_maksbredde, 'testing')}>{harInnhold ? children : null}</div>;
};

export default FaneTiltaksinformasjon;
