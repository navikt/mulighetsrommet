import { BodyShort, Heading, Label } from '@navikt/ds-react';
import styles from './Kontaktinfo.module.scss';

interface ArrangorInfoProps {
  data: any;
}

const ArrangorInfo = ({ data }: ArrangorInfoProps) => {
  const { kontaktinfoArrangor } = data;

  return kontaktinfoArrangor ? (
    <div>
      <Heading size="medium" level="2" className={styles.header}>
        Arrangør
      </Heading>
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
    </div>
  ) : (
    <></>
  );
};
export default ArrangorInfo;
