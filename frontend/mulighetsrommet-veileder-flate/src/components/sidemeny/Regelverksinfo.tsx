import { Link } from '@navikt/ds-react';
import { SanityRegelverkFil, SanityRegelverkLenke } from 'mulighetsrommet-api-client';
import { logEvent } from '../../core/api/logger';
import styles from './Sidemenydetaljer.module.scss';

interface RegelverksinfoProps {
  regelverkFiler?: SanityRegelverkFil[];
  regelverkLenker?: SanityRegelverkLenke[];
}

const Regelverksinfo = ({ regelverkFiler, regelverkLenker }: RegelverksinfoProps) => {
  const loggTrykkPaRegelverk = () => logEvent('mulighetsrommet.regelverk');

  const regelverkFilComponent = (regelverkFil: SanityRegelverkFil) => {
    return (
      regelverkFil.regelverkFilUrl && (
        <div key={regelverkFil._id}>
          <Link target="_blank" href={`${regelverkFil.regelverkFilUrl}`} onClick={loggTrykkPaRegelverk}>
            {regelverkFil.regelverkFilNavn}
          </Link>
        </div>
      )
    );
  };

  const regelverkLenkeComponent = (regelverkLenke: SanityRegelverkLenke) => {
    return (
      regelverkLenke.regelverkUrl && (
        <div key={regelverkLenke._id}>
          <Link target="_blank" href={regelverkLenke.regelverkUrl} onClick={loggTrykkPaRegelverk}>
            {regelverkLenke.regelverkLenkeNavn}
          </Link>
        </div>
      )
    );
  };

  return (
    <div className={styles.regelverk}>
      {regelverkFiler && regelverkFiler.map(regelverkFilComponent)}
      {regelverkLenker && regelverkLenker.map(regelverkLenkeComponent)}
    </div>
  );
};

export default Regelverksinfo;
