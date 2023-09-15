import { Alert, BodyShort, Heading } from '@navikt/ds-react';
import { SanityKontakinfoTiltaksansvarlige, VeilederflateTiltaksgjennomforing } from 'mulighetsrommet-api-client';
import { logEvent } from '../../../core/api/logger';
import styles from './Kontaktinfo.module.scss';

const TEAMS_DYPLENKE = 'https://teams.microsoft.com/l/chat/0/0?users=';

interface NavKontaktpersonInfoProps {
  data: VeilederflateTiltaksgjennomforing;
}

const NavKontaktpersonInfo = ({ data }: NavKontaktpersonInfoProps) => {
  const { kontaktinfoTiltaksansvarlige: tiltaksansvarlige } = data;

  if (tiltaksansvarlige?.length === 0 || !tiltaksansvarlige)
    return (
      <Alert variant="info" inline>
        Kontaktinfo til tiltaksansvarlig er ikke lagt inn
      </Alert>
    );

  return (
    <div className={styles.tiltaksansvarlig_info}>
      <Heading size="small" className={styles.header}>
        Tiltaksansvarlig
      </Heading>

      {tiltaksansvarlige.map((tiltaksansvarlig: SanityKontakinfoTiltaksansvarlige) => {
        return (
          <div key={tiltaksansvarlig.epost} className={styles.container}>
            <BodyShort className={styles.navn} size="small">
              {tiltaksansvarlig.navn}
            </BodyShort>

            <BodyShort size="small">
              <div className={styles.infofelt}>
                <div className={styles.kolonne}>
                  <span>Telefon:</span>
                  <span>Epost:</span>
                  <span>Teams:</span>
                  <span>Enhet:</span>
                </div>

                <div className={styles.kolonne}>
                  <span>{tiltaksansvarlig.telefonnummer}</span>
                  <a
                    href={`mailto:${tiltaksansvarlig.epost}`}
                    onClick={() => logEvent('mulighetsrommet.tiltaksansvarlig.epost')}
                  >
                    {tiltaksansvarlig.epost}
                  </a>
                  <a
                    target="_blank"
                    rel="noreferrer"
                    href={`${TEAMS_DYPLENKE}${encodeURIComponent(tiltaksansvarlig.epost)}`}
                    onClick={() => logEvent('mulighetsrommet.tiltaksansvarlig.teamslenke')}
                  >
                    Kontakt meg på Teams
                  </a>
                  <span>{tiltaksansvarlig.enhet}</span>
                </div>
              </div>
            </BodyShort>
          </div>
        );
      })}
    </div>
  );
};

export default NavKontaktpersonInfo;
