import { BodyShort, Heading, Label } from '@navikt/ds-react';
import styles from './Kontaktinfo.module.scss';

interface ArrangorInfoProps {
  data: any;
}

const ArrangorInfo = ({ data }: ArrangorInfoProps) => {
  const { kontaktinfoArrangor } = data;

  return kontaktinfoArrangor ? (
    <div className={styles.arrangor_info}>
      <Heading size="small" level="2" className={styles.header}>
        Arrangør
      </Heading>
      <div className={styles.container}>
        <Label>{kontaktinfoArrangor?.selskapsnavn}</Label>
        <div className={styles.infofelt}>
          <div className={styles.rad}>
            <BodyShort>Telefon</BodyShort>
            <BodyShort>{kontaktinfoArrangor?.telefonnummer}</BodyShort>
          </div>
          <div className={styles.rad}>
            <BodyShort>Adresse</BodyShort>
            <BodyShort>{kontaktinfoArrangor?.adresse}</BodyShort>
          </div>
        </div>
      </div>
    </div>
  ) : (
    <></>
  );
};
export default ArrangorInfo;
