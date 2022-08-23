import { ReactNode } from 'react';
import './Feilmelding.less';
import { ErrorColored, InformationColored, WarningColored } from '@navikt/ds-icons';

interface FeilmeldingProps {
  children: ReactNode;
  ikonvariant?: string;
}

export function Feilmelding({ children, ikonvariant }: FeilmeldingProps) {
  const ikon = () => {
    if (ikonvariant === 'info') {
      return <InformationColored />;
    } else if (ikonvariant === 'warning') {
      return <WarningColored />;
    } else if (ikonvariant === 'error') {
      return <ErrorColored />;
    }
  };
  return (
    <div data-testid="feilmelding-container" aria-live="assertive" className="feilmelding-container">
      {ikon()}
      {children}
    </div>
  );
}
