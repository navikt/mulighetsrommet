import { Alert, BodyShort } from '@navikt/ds-react';
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
      <BodyShort size="medium" className={styles.header}>
        Tiltaksansvarlig
      </BodyShort>
      {tiltaksansvarlige.map((tiltaksansvarlig: SanityKontakinfoTiltaksansvarlige) => {
        return (
          <div className={styles.container} key={tiltaksansvarlig._id}>
            <BodyShort className={styles.navn} size="small">
              {tiltaksansvarlig.navn}
            </BodyShort>
            <div className={styles.infofelt}>
              <div className={styles.rad}>
                <BodyShort size="small">Telefon</BodyShort>
                <BodyShort size="small">{tiltaksansvarlig.telefonnummer}</BodyShort>
              </div>
              <div className={styles.rad}>
                <BodyShort size="small">Epost</BodyShort>
                <BodyShort size="small">
                  <a
                    href={`mailto:${tiltaksansvarlig.epost}`}
                    onClick={() => logEvent('mulighetsrommet.tiltaksansvarlig.epost')}
                  >
                    {tiltaksansvarlig.epost}
                  </a>
                </BodyShort>
              </div>
              <div className={styles.rad}>
                <BodyShort size="small">Teams</BodyShort>
                <BodyShort size="small">
                  <a
                    target="_blank"
                    rel="noreferrer"
                    href={`${TEAMS_DYPLENKE}${encodeURIComponent(tiltaksansvarlig.epost)}`}
                    onClick={() => logEvent('mulighetsrommet.tiltaksansvarlig.teamslenke')}
                  >
                    Kontakt meg på Teams
                  </a>
                </BodyShort>
              </div>
              <div className={styles.rad}>
                <BodyShort size="small">Enhet</BodyShort>
                <BodyShort size="small">{tiltaksansvarlig.enhet}</BodyShort>
              </div>
            </div>
          </div>
        );
      })}
    </div>
  );
};

export default NavKontaktpersonInfo;
