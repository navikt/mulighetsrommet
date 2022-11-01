import { ReactNode } from 'react';
import styles from './Feilmelding.module.scss';
import { ErrorColored, InformationColored, WarningColored } from '@navikt/ds-icons';
import { BodyShort, Heading } from '@navikt/ds-react';

interface FeilmeldingProps {
  header: ReactNode;
  beskrivelse: ReactNode;
  children?: ReactNode;
  ikonvariant?: string;
}

export const forsokPaNyttLink = () => {
  return <a href=".">forsøk på nytt</a>;
};

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
      <Heading level="4" size={'small'} className={styles.header}>
        {header}
      </Heading>
      <BodyShort size={'small'} className={styles.beskrivelse}>
        {beskrivelse}
      </BodyShort>
      {children}
    </div>
  );
};
