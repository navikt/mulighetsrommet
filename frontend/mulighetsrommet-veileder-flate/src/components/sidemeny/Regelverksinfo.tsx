import React from 'react';
import { RegelverkFil, RegelverkLenke } from '../../api/models';
import {Link} from "@navikt/ds-react";

interface RegelverksinfoProps {
  regelverkFiler: RegelverkFil[];
  regelverkLenker: RegelverkLenke[];
}

const Regelverksinfo = ({ regelverkFiler, regelverkLenker }: RegelverksinfoProps) => {
  return (
    <div className="tiltakstype-detaljer__regelverk">
      {regelverkFiler && regelverkFiler.map(regelverkFilComponent)}
      {regelverkLenker && regelverkLenker.map(regelverkLenkeComponent)}
    </div>
  );
};

function regelverkFilComponent(regelverkFil: RegelverkFil) {
  console.log(regelverkFil)
  return (
    regelverkFil.regelverkFilUrl && (
      <div key={regelverkFil._id}>
        <Link
          target="_blank"
          href={`${regelverkFil.regelverkFilUrl}`}
        >
          {regelverkFil.regelverkFilNavn}
        </Link>
      </div>
    )
  );
}

function regelverkLenkeComponent(regelverkLenke: RegelverkLenke) {
  return (
    regelverkLenke.regelverkurl && (
      <div key={regelverkLenke._id}>
        <Link
          target="_blank"
          href={regelverkLenke.regelverkurl}
        >
          {regelverkLenke.regelverkLenkeNavn}
        </Link>
      </div>
    )
  );
}

/*
function tiltaksansvarligComponent(tiltaksansvarlig: Tiltaksansvarlig) {
  return (
    <div className="kontaktinfo__header" key={tiltaksansvarlig._id}>
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
}
*/
export default Regelverksinfo;
