import { Alert, BodyShort, Heading, Label } from '@navikt/ds-react';
import styles from './Kontaktinfo.module.scss';
import { Tiltaksansvarlig } from '../../../core/api/models';
import { logEvent } from '../../../core/api/logger';

const TEAMS_DYPLENKE = 'https://teams.microsoft.com/l/chat/0/0?users=';

interface TiltaksansvarligInfoProps {
  data: any;
}

const TiltaksansvarligInfo = ({ data }: TiltaksansvarligInfoProps) => {
  const { kontaktinfoTiltaksansvarlige: tiltaksansvarlige } = data;
  if (tiltaksansvarlige?.length === 0 || !tiltaksansvarlige)
    return (
      <Alert variant="info" inline>
        Kontaktinfo til tiltaksansvarlig er ikke lagt inn
      </Alert>
    );

  return (
    <div>
      <Heading size="medium" level="2" className={styles.header}>
        Tiltaksansvarlig
      </Heading>
      {tiltaksansvarlige.map((tiltaksansvarlig: Tiltaksansvarlig) => {
        return (
          <div className={styles.container} key={tiltaksansvarlig._id}>
            <Heading size="small" level="3" className={styles.navn}>
              {tiltaksansvarlig.navn}
            </Heading>
            <div className={styles.container}>
              <div className={styles.rad}>
                <Label size="small">Telefon</Label>
                <BodyShort>{tiltaksansvarlig.telefonnummer}</BodyShort>
              </div>
              <div className={styles.rad}>
                <Label size="small">Epost</Label>
                <BodyShort>
                  <a
                    href={`mailto:${tiltaksansvarlig.epost}`}
                    onClick={() => logEvent('mulighetsrommet.tiltaksansvarlig.epost')}
                  >
                    {tiltaksansvarlig.epost}
                  </a>
                </BodyShort>
              </div>
              <div className={styles.rad}>
                <Label size="small">Teams</Label>
                <BodyShort>
                  <a
                    target="_blank"
                    rel="noreferrer"
                    href={`${TEAMS_DYPLENKE}${encodeURIComponent(tiltaksansvarlig.epost)}`}
                    onClick={() => logEvent('mulighetsrommet.tiltaksansvarlig.teamslenke')}
                  >
                    Kontakt {tiltaksansvarlig.navn} på Teams
                  </a>
                </BodyShort>
              </div>
              <div className={styles.rad}>
                <Label size="small">Enhet</Label>
                <BodyShort>{tiltaksansvarlig.enhet}</BodyShort>
              </div>
            </div>
          </div>
        );
      })}
    </div>
  );
};

export default TiltaksansvarligInfo;
