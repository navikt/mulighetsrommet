import ArrangorInfo from './ArrangorInfo';
import styles from './Kontaktinfo.module.scss';
import TiltaksansvarligInfo from './TiltaksansvarligInfo';
import FaneTiltaksinformasjon from '../FaneTiltaksinformasjon';
import useTiltaksgjennomforingById from '../../../core/api/queries/useTiltaksgjennomforingById';

const KontaktinfoFane = () => {
  const { data } = useTiltaksgjennomforingById();
  return (
    <FaneTiltaksinformasjon harInnhold={!!data} className={styles.kontaktinfo_container}>
      <ArrangorInfo data={data} />
      <TiltaksansvarligInfo data={data} />
    </FaneTiltaksinformasjon>
  );
};

export default KontaktinfoFane;
