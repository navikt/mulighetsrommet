import classNames from 'classnames';
import { ReactNode } from 'react';
import modalStyles from '../Modal.module.scss';

export function Infomelding({ children }: { children: ReactNode }) {
  return <p className={classNames(modalStyles.muted, modalStyles.mb_0)}>{children}</p>;
}
