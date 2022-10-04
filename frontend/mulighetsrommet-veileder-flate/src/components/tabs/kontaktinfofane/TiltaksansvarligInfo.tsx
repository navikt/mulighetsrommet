import { BodyShort, Heading, Label } from '@navikt/ds-react';
import React from 'react';
import styles from './KontaktinfoFane.module.scss';
import { Tiltaksansvarlig } from '../../../core/api/models';

const TEAMS_DYPLENKE = 'https://teams.microsoft.com/l/chat/0/0?users=';

interface TiltaksansvarligProps {
  tiltaksansvarlige: Tiltaksansvarlig[];
}

const TiltaksansvarligInfo = ({ tiltaksansvarlige }: TiltaksansvarligProps) => {
  return (
    <>
      {tiltaksansvarlige.map(tiltaksansvarlig => {
        return (
          <div className={styles.kontaktinfo__container} key={tiltaksansvarlig._id}>
            <Heading size="small" level="3" className={styles.kontaktinfo__navn}>
              {tiltaksansvarlig.navn}
            </Heading>
            <div className={styles.kontaktinfo__container}>
              <div className={styles.kontaktinfo__rad}>
                <Label size="small">Telefon</Label>
                <BodyShort>{tiltaksansvarlig.telefonnummer}</BodyShort>
              </div>
              <div className={styles.kontaktinfo__rad}>
                <Label size="small">Epost</Label>
                <BodyShort>
                  <a href={`mailto:${tiltaksansvarlig.epost}`}>{tiltaksansvarlig.epost}</a>
                </BodyShort>
              </div>
              <div className={styles.kontaktinfo__rad}>
                <Label size="small">Teams</Label>
                <BodyShort>
                  <a target="_blank" href={`${TEAMS_DYPLENKE}${encodeURIComponent(tiltaksansvarlig.epost)}`}>
                    Kontakt {tiltaksansvarlig.navn} på Teams
                  </a>
                </BodyShort>
              </div>
              <div className={styles.kontaktinfo__rad}>
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
