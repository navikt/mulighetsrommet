import { Heading } from '@navikt/ds-react';
import { Arrangor, Tiltaksansvarlig } from '../../../core/api/models';
import ArrangorInfo from './ArrangorInfo';
import styles from './KontaktinfoFane.module.scss';
import TiltaksansvarligInfo from './TiltaksansvarligInfo';

interface KontaktinfoFaneProps {
  tiltaksansvarlige: Tiltaksansvarlig[];
  arrangorinfo?: Arrangor;
}

const KontaktinfoFane = ({ tiltaksansvarlige, arrangorinfo }: KontaktinfoFaneProps) => {
  return (
    <div className={styles.kontaktinfo}>
      <div>
        <Heading size="large" level="2" className={styles.header}>
          Arrangør
        </Heading>
        {arrangorinfo ? <ArrangorInfo arrangorinfo={arrangorinfo} /> : null}
      </div>
      <div>
        <Heading size="large" level="2" className={styles.header}>
          Tiltaksansvarlig
        </Heading>
        <TiltaksansvarligInfo tiltaksansvarlige={tiltaksansvarlige} />
      </div>
    </div>
  );
};

export default KontaktinfoFane;
