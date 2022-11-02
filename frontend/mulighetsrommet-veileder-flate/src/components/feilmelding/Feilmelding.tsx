import { ReactNode } from 'react';
import styles from './Feilmelding.module.scss';
import { ErrorColored, InformationColored, WarningColored } from '@navikt/ds-icons';
import { BodyShort, Heading } from '@navikt/ds-react';

interface FeilmeldingProps {
  header: ReactNode;
  beskrivelse: ReactNode;
  children?: ReactNode;
  ikonvariant?: string;
  medContainer?: boolean;
}

export const forsokPaNyttLink = () => {
  return <a href=".">forsøk på nytt</a>;
};

export const Feilmelding = ({ header, beskrivelse, children, ikonvariant, medContainer }: FeilmeldingProps) => {
  const ikon = () => {
    if (ikonvariant === 'info') {
      return <InformationColored />;
    } else if (ikonvariant === 'warning') {
      return <WarningColored />;
    } else if (ikonvariant === 'error') {
      return <ErrorColored />;
    }
  };

  const innhold = () => {
    return (
      <>
        {ikon()}
        <Heading level="4" size={'small'} className={styles.header}>
          {header}
        </Heading>
        <BodyShort size={'small'} className={styles.beskrivelse}>
          {beskrivelse}
        </BodyShort>
        {children}
      </>
    );
  };

  return medContainer !== false ? (
    <div data-testid="feilmelding-container" aria-live="assertive" className={styles.feilmelding_container}>
      {innhold()}
    </div>
  ) : (
    <>{innhold()}</>
  );
};
