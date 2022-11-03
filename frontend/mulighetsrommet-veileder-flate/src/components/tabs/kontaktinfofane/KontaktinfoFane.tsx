import { Heading } from '@navikt/ds-react';
import ArrangorInfo from './ArrangorInfo';
import styles from './KontaktinfoFane.module.scss';
import TiltaksansvarligInfo from './TiltaksansvarligInfo';

const KontaktinfoFane = () => {
  return (
    <div className={styles.kontaktinfo}>
      <div>
        <Heading size="medium" level="2" className={styles.header}>
          Arrangør
        </Heading>
        <ArrangorInfo />
      </div>
      <div>
        <Heading size="medium" level="2" className={styles.header}>
          Tiltaksansvarlig
        </Heading>
        <TiltaksansvarligInfo />
      </div>
    </div>
  );
};

export default KontaktinfoFane;
