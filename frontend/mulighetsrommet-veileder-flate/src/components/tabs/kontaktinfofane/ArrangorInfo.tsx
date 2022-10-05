import { BodyShort, Heading, Label } from '@navikt/ds-react';
import useTiltaksgjennomforingByTiltaksnummer from '../../../core/api/queries/useTiltaksgjennomforingByTiltaksnummer';
import styles from './Arrangorinfo.module.scss';

const ArrangorInfo = () => {
  const { data } = useTiltaksgjennomforingByTiltaksnummer();
  if (!data) return null;

  const { kontaktinfoArrangor } = data;

  if (!kontaktinfoArrangor) return null;

  return (
    <>
      <Heading size="small" level="3" className={styles.navn}>
        {kontaktinfoArrangor?.selskapsnavn}
      </Heading>
      <div className={styles.container}>
        <div className={styles.rad}>
          <Label size="small">Telefon</Label>
          <BodyShort>{kontaktinfoArrangor?.telefonnummer}</BodyShort>
        </div>
        <div className={styles.rad}>
          <Label size="small">Adresse</Label>
          <BodyShort>{kontaktinfoArrangor?.adresse}</BodyShort>
        </div>
      </div>
    </>
  );
};
export default ArrangorInfo;
