import { Heading } from '@navikt/ds-react';
import ArrangorInfo from './ArrangorInfo';
import styles from './KontaktinfoFane.module.scss';
import TiltaksansvarligInfo from './TiltaksansvarligInfo';
import FaneTiltaksinformasjon from '../FaneTiltaksinformasjon';
import useTiltaksgjennomforingById from '../../../core/api/queries/useTiltaksgjennomforingById';

const KontaktinfoFane = () => {
  const { data } = useTiltaksgjennomforingById();

  return (
    <FaneTiltaksinformasjon harInnhold={!!data}>
      <div className={styles.kontaktinfo}>
        <div>
          <Heading size="medium" level="2" className={styles.header}>
            Arrangør
          </Heading>
          <ArrangorInfo data={data} />
        </div>
        <div>
          <Heading size="medium" level="2" className={styles.header}>
            Tiltaksansvarlig
          </Heading>
          <TiltaksansvarligInfo data={data} />
        </div>
      </div>
    </FaneTiltaksinformasjon>
  );
};

export default KontaktinfoFane;
