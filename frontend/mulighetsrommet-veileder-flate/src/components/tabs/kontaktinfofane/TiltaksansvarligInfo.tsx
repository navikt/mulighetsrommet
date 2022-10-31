import { BodyShort, Heading, Label } from '@navikt/ds-react';
import useTiltaksgjennomforingById from '../../../core/api/queries/useTiltaksgjennomforingById';
import styles from './Arrangorinfo.module.scss';

const TEAMS_DYPLENKE = 'https://teams.microsoft.com/l/chat/0/0?users=';

const TiltaksansvarligInfo = () => {
  const { data } = useTiltaksgjennomforingById();
  if (!data) return null;

  const { kontaktinfoTiltaksansvarlige: tiltaksansvarlige } = data;
  return (
    <>
      {tiltaksansvarlige.map(tiltaksansvarlig => {
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
                  <a href={`mailto:${tiltaksansvarlig.epost}`}>{tiltaksansvarlig.epost}</a>
                </BodyShort>
              </div>
              <div className={styles.rad}>
                <Label size="small">Teams</Label>
                <BodyShort>
                  <a target="_blank" href={`${TEAMS_DYPLENKE}${encodeURIComponent(tiltaksansvarlig.epost)}`}>
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
    </>
  );
};

export default TiltaksansvarligInfo;
