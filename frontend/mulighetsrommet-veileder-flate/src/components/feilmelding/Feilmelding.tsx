import { ReactNode } from 'react';
import styles from './Feilmelding.module.scss';
import { ErrorColored, InformationColored, WarningColored } from '@navikt/ds-icons';
import { BodyShort, Heading } from '@navikt/ds-react';
import classNames from 'classnames';

interface FeilmeldingProps {
  header: ReactNode;
  beskrivelse: ReactNode;
  children?: ReactNode;
  ikonvariant?: string;
  utenMargin?: boolean;
}

export const forsokPaNyttLink = () => {
  return <a href=".">forsøk på nytt</a>;
};

export const Feilmelding = ({ header, beskrivelse, children, ikonvariant, utenMargin }: FeilmeldingProps) => {
  const ikon = () => {
    if (ikonvariant === 'info') {
      return <InformationColored />;
    } else if (ikonvariant === 'warning') {
      return <WarningColored />;
    } else if (ikonvariant === 'error') {
      return <ErrorColored />;
    }
  };

  const classNamesArray = utenMargin
    ? [styles.feilmelding_container]
    : [styles.feilmelding_container, styles.feilmelding_margin];

  return (
    <div data-testid="feilmelding-container" aria-live="assertive" className={classNames(...classNamesArray)}>
      {ikon()}
      <Heading level="4" size={'small'}>
        {header}
      </Heading>
      <BodyShort size={'small'} className={styles.beskrivelse}>
        {beskrivelse}
      </BodyShort>
      {children}
    </div>
  );
};
