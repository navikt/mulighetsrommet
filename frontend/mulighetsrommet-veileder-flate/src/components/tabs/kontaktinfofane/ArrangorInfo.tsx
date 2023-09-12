import { BodyShort } from '@navikt/ds-react';
import styles from './Kontaktinfo.module.scss';
import { VeilederflateTiltaksgjennomforing } from 'mulighetsrommet-api-client';
import { logEvent } from '../../../core/api/logger';

interface ArrangorInfoProps {
  data: VeilederflateTiltaksgjennomforing;
}

const ArrangorInfo = ({ data }: ArrangorInfoProps) => {
  const { kontaktinfoArrangor } = data;

  if (!kontaktinfoArrangor) {
    return null;
  }

  const { kontaktperson } = kontaktinfoArrangor;

  return (
    <div className={styles.arrangor_info}>
      <BodyShort size="medium" className={styles.header}>
        Arrangør
      </BodyShort>
      <div className={styles.container}>
        <BodyShort className={styles.navn} size="small">
          {kontaktinfoArrangor.selskapsnavn}
        </BodyShort>
        <div className={styles.infofelt}>
          <div className={styles.rad}>
            <BodyShort size="small">Lokasjon</BodyShort>
            <BodyShort size="small">{kontaktinfoArrangor.lokasjon}</BodyShort>
          </div>
        </div>
      </div>

      {kontaktperson && (
        <div className={styles.container}>
          <BodyShort className={styles.navn} size="small">
            {kontaktperson.navn}
          </BodyShort>
          <div className={styles.infofelt}>
            <div className={styles.rad}>
              <BodyShort size="small">Telefon</BodyShort>
              <BodyShort size="small">{kontaktperson.telefon}</BodyShort>
            </div>
            <div className={styles.rad}>
              <BodyShort size="small">Epost</BodyShort>
              <BodyShort size="small">
                <a
                  href={`mailto:${kontaktperson.epost}`}
                  onClick={() => logEvent('mulighetsrommet.arrangor.kontaktperson.epost')}
                >
                  {kontaktperson.epost}
                </a>
              </BodyShort>
            </div>
          </div>
        </div>
      )}
    </div>
  );
};
export default ArrangorInfo;
