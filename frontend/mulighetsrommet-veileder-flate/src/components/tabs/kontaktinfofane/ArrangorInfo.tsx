import { BodyShort, Heading } from '@navikt/ds-react';
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
      <Heading size="small" className={styles.header}>
        Arrangør
      </Heading>

      <div className={styles.container}>
        <BodyShort className={styles.navn} size="small">
          {kontaktinfoArrangor.selskapsnavn}
        </BodyShort>

        <BodyShort size="small">
          <div className={styles.infofelt}>
            <div className={styles.kolonne}>Lokasjon:</div>
            <div className={styles.kolonne}>{kontaktinfoArrangor.lokasjon}</div>
          </div>
        </BodyShort>
      </div>

      {kontaktperson && (
        <div className={styles.container}>
          <BodyShort className={styles.navn} size="small">
            {kontaktperson.navn}
          </BodyShort>

          <BodyShort size="small">
            <div className={styles.infofelt}>
              <div className={styles.kolonne}>
                <span>Telefon:</span>
                <span>Epost:</span>
              </div>

              <div className={styles.kolonne}>
                <span>{kontaktperson.telefon}</span>
                <a
                  href={`mailto:${kontaktperson.epost}`}
                  onClick={() => logEvent('mulighetsrommet.arrangor.kontaktperson.epost')}
                >
                  {kontaktperson.epost}
                </a>
              </div>
            </div>
          </BodyShort>
        </div>
      )}
    </div>
  );
};
export default ArrangorInfo;
