import { ReactNode } from 'react';
import './Feilmelding.less';

interface Props {
  children: ReactNode;
}

export function Feilmelding({ children }: Props) {
  return (
    <div data-testid="feilmelding-container" aria-live="assertive" className="feilmelding-container">
      {children}
    </div>
  );
}
