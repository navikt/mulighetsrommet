import { BodyShort, Heading, Label } from '@navikt/ds-react';
import React from 'react';
import './KontaktinfoFane.less';
import { Tiltaksansvarlig } from '../../../core/api/models';

interface TiltaksansvarligProps {
  tiltaksansvarlige: Tiltaksansvarlig[];
}

const TiltaksansvarligInfo = ({ tiltaksansvarlige }: TiltaksansvarligProps) => {
  return (
    <>
      {tiltaksansvarlige.map(tiltaksansvarlig => {
        return (
          <div className="kontaktinfo__container" key={tiltaksansvarlig._id}>
            <Heading size="small" level="3" className="kontaktinfo__navn">
              {tiltaksansvarlig.navn}
            </Heading>
            <div className="kontaktinfo__container">
              <div className="kontaktinfo__rad">
                <Label size="small">Telefon</Label>
                <BodyShort>{tiltaksansvarlig.telefonnummer}</BodyShort>
              </div>
              <div className="kontaktinfo__rad">
                <Label size="small">Epost</Label>
                <BodyShort>
                  <a href={`mailto:${tiltaksansvarlig.epost}`}>{tiltaksansvarlig.epost}</a>
                </BodyShort>
              </div>
              <div className="kontaktinfo__rad">
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
