import ArrangorInfo from './ArrangorInfo';
import styles from './Kontaktinfo.module.scss';
import TiltaksansvarligInfo from './TiltaksansvarligInfo';
import FaneTiltaksinformasjon from '../FaneTiltaksinformasjon';
import useTiltaksgjennomforingById from '../../../core/api/queries/useTiltaksgjennomforingById';
import { erPreview } from '../../../utils/Utils';
import { Alert } from '@navikt/ds-react';

const KontaktinfoFane = () => {
  const { data } = useTiltaksgjennomforingById();
  return (
    <FaneTiltaksinformasjon harInnhold={!!data} className={styles.kontaktinfo_container}>
      {erPreview ? (
        <Alert variant="info">Ved forhåndsvisning vises ikke kontaktinformasjon</Alert>
      ) : (
        <>
          <ArrangorInfo data={data} />
          <TiltaksansvarligInfo data={data} />
        </>
      )}
    </FaneTiltaksinformasjon>
  );
};

export default KontaktinfoFane;
