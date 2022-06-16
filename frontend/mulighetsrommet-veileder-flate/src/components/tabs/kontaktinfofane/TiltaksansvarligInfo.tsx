import { BodyShort, Heading, Label } from '@navikt/ds-react';
import React from 'react';
import './KontaktinfoFane.less';
import { Tiltaksansvarlig } from '../../../api/models';

interface TiltaksansvarligProps {
  tiltaksansvarlig: Tiltaksansvarlig;
}

const TiltaksansvarligInfo = ({ tiltaksansvarlig }: TiltaksansvarligProps) => {
  //TODO når det er mulig å ha flere tiltaksansvarlige i Sanity må det mappes her
  return (
    <>
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
          <BodyShort>{tiltaksansvarlig.epost}</BodyShort>
        </div>
        <div className="kontaktinfo__rad">
          <Label size="small">Enhet</Label>
          <BodyShort>{tiltaksansvarlig.enhet}</BodyShort>
        </div>
      </div>
    </>
  );
};
export default TiltaksansvarligInfo;
