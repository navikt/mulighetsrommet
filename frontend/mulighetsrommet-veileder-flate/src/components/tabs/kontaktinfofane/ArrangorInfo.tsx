import { BodyShort, Heading } from '@navikt/ds-react';
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
        <BodyShort className={styles.navn} size="small">
          {kontaktinfoArrangor?.selskapsnavn}
        </BodyShort>
        <div className={styles.infofelt}>
          <div className={styles.rad}>
            <BodyShort size="small">Telefon</BodyShort>
            <BodyShort size="small">{kontaktinfoArrangor?.telefonnummer}</BodyShort>
          </div>
          <div className={styles.rad}>
            <BodyShort size="small">Adresse</BodyShort>
            <BodyShort size="small">{kontaktinfoArrangor?.adresse}</BodyShort>
          </div>
        </div>
      </div>
    </div>
  ) : (
    <></>
  );
};
export default ArrangorInfo;
