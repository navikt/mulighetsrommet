import { BodyShort, Heading, Label } from '@navikt/ds-react';
import React from 'react';

interface ArrangorProps {
  arrangorinfo: any;
}

const ArrangorInfo = ({ arrangorinfo }: ArrangorProps) => {
  return (
    <>
      <Heading size="small" className="kontaktinfo__navn">
        {arrangorinfo.selskapsnavn}
      </Heading>
      <div className="kontaktinfo__container">
        <div className="kontaktinfo__rad">
          <Label size="small">Telefon</Label>
          <BodyShort>{arrangorinfo.telefonnummer}</BodyShort>
        </div>
        <div className="kontaktinfo__rad">
          <Label size="small">Epost</Label>
          <BodyShort>{arrangorinfo.epost}</BodyShort>
        </div>
        <div className="kontaktinfo__rad">
          <Label size="small">Adresse</Label>
          <BodyShort>{arrangorinfo.adresse}</BodyShort>
        </div>
      </div>
    </>
  );
};
export default ArrangorInfo;
