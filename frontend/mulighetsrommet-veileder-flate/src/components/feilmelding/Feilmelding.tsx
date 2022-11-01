import { ReactNode } from 'react';
import styles from './Feilmelding.module.scss';
import { ErrorColored, InformationColored, WarningColored } from '@navikt/ds-icons';
import { BodyShort, Heading } from '@navikt/ds-react';

interface FeilmeldingProps {
  header?: string;
  beskrivelse?: string;
  children?: ReactNode;
  ikonvariant?: string;
}

export const Feilmelding = ({ header, beskrivelse, children, ikonvariant }: FeilmeldingProps) => {
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
    <div data-testid="feilmelding-container" aria-live="assertive" className={styles.feilmelding_container}>
      {ikon()}
      <Heading size={'small'}>{header}</Heading>
      <BodyShort size={'small'}>{beskrivelse}</BodyShort>
      {children}
    </div>
  );
};
