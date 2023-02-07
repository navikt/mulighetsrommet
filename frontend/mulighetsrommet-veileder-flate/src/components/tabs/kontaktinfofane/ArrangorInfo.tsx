import { Alert, BodyShort, Heading, Label } from '@navikt/ds-react';
import styles from './Arrangorinfo.module.scss';

interface ArrangorInfoProps {
  data: any;
}

const ArrangorInfo = ({ data }: ArrangorInfoProps) => {
  const { kontaktinfoArrangor } = data;

  if (!kontaktinfoArrangor)
    return (
      <Alert variant="info" inline>
        Konktaktinfo til arrangør er ikke lagt inn
      </Alert>
    );

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
