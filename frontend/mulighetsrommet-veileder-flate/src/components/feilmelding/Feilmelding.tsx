import { ReactNode } from 'react';
import './Feilmelding.less';

interface Props {
  children: ReactNode;
}

export function Feilmelding({ children }: Props) {
  return (
    <div aria-live="assertive" className="feilmelding-container">
      {children}
    </div>
  );
}
