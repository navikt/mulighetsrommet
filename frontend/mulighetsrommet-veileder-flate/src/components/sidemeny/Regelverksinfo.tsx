import React from 'react';
import { RegelverkFil, RegelverkLenke } from '../../api/models';
import { Link } from '@navikt/ds-react';
import { logEvent } from '../../api/logger';

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

function loggTrykkPaRegelverk() {
  logEvent('mulighetsrommet.regelverk');
}

function regelverkFilComponent(regelverkFil: RegelverkFil) {
  return (
    regelverkFil.regelverkFilUrl && (
      <div key={regelverkFil._id}>
        <Link target="_blank" href={`${regelverkFil.regelverkFilUrl}`} onClick={loggTrykkPaRegelverk}>
          {regelverkFil.regelverkFilNavn}
        </Link>
      </div>
    )
  );
}

function regelverkLenkeComponent(regelverkLenke: RegelverkLenke) {
  return (
    regelverkLenke.regelverkUrl && (
      <div key={regelverkLenke._id}>
        <Link target="_blank" href={regelverkLenke.regelverkUrl} onClick={loggTrykkPaRegelverk}>
          {regelverkLenke.regelverkLenkeNavn}
        </Link>
      </div>
    )
  );
}

export default Regelverksinfo;
