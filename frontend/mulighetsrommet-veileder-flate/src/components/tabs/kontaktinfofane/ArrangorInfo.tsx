import { BodyShort, Heading, Label } from '@navikt/ds-react';
import useTiltaksgjennomforingById from '../../../core/api/queries/useTiltaksgjennomforingById';
import styles from './Arrangorinfo.module.scss';

const ArrangorInfo = () => {
  const { data } = useTiltaksgjennomforingById();
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
