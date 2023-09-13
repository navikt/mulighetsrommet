import { Alert } from '@navikt/ds-react';
import { SanityTiltaksgjennomforing } from 'mulighetsrommet-api-client';
import { erPreview } from '../../../utils/Utils';
import FaneTiltaksinformasjon from '../FaneTiltaksinformasjon';
import ArrangorInfo from './ArrangorInfo';
import styles from './Kontaktinfo.module.scss';
import TiltaksansvarligInfo from './TiltaksansvarligInfo';

interface Props {
  tiltaksgjennomforing: SanityTiltaksgjennomforing;
}

const KontaktinfoFane = ({ tiltaksgjennomforing }: Props) => {
  return (
    <FaneTiltaksinformasjon harInnhold={!!tiltaksgjennomforing} className={styles.kontaktinfo_container}>
      {erPreview ? (
        <Alert variant="info">Ved forhåndsvisning vises ikke kontaktinformasjon</Alert>
      ) : (
        <>
          <ArrangorInfo data={tiltaksgjennomforing} />
          <TiltaksansvarligInfo data={tiltaksgjennomforing} />
        </>
      )}
    </FaneTiltaksinformasjon>
  );
};

export default KontaktinfoFane;
