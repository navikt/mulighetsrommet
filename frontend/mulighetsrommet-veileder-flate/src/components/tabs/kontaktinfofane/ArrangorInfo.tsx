import { BodyShort, Heading, Label } from '@navikt/ds-react';
import { Arrangor } from '../../../core/api/models';
import styles from './Arrangorinfo.module.scss';

interface ArrangorProps {
  arrangorinfo: Arrangor;
}

const ArrangorInfo = ({ arrangorinfo }: ArrangorProps) => {
  return (
    <>
      <Heading size="small" level="3" className={styles.navn}>
        {arrangorinfo.selskapsnavn}
      </Heading>
      <div className={styles.container}>
        <div className={styles.rad}>
          <Label size="small">Telefon</Label>
          <BodyShort>{arrangorinfo.telefonnummer}</BodyShort>
        </div>
        <div className={styles.rad}>
          <Label size="small">Adresse</Label>
          <BodyShort>{arrangorinfo.adresse}</BodyShort>
        </div>
      </div>
    </>
  );
};
export default ArrangorInfo;
