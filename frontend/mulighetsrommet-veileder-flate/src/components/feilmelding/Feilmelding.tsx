import { ReactNode } from 'react';
import './Feilmelding.less';
import { WarningColored } from '@navikt/ds-icons';

interface Props {
  children: ReactNode;
}

export function Feilmelding({ children }: Props) {
  return (
    <div data-testid="feilmelding-container" aria-live="assertive" className="feilmelding-container">
      <WarningColored />
      {children}
    </div>
  );
}
