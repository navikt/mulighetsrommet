/* eslint-disable camelcase */
import { SanityTiltaksgjennomforing } from 'mulighetsrommet-api-client';
import StatusGronn from '../../ikoner/Sirkel-gronn.png';
import StatusGul from '../../ikoner/Sirkel-gul.png';
import StatusRod from '../../ikoner/Sirkel-rod.png';
import { formaterDato } from '../../utils/Utils';
import styles from './Tilgjengelighetsstatus.module.scss';

interface Props {
  status?: SanityTiltaksgjennomforing.tilgjengelighetsstatus;
  estimert_ventetid?: string;
  stengtFra?: string;
  stengtTil?: string;
}

export function TilgjengelighetsstatusComponent({ status, estimert_ventetid, stengtFra, stengtTil }: Props) {
  const todayDate = new Date();

  if (stengtFra && stengtTil && todayDate <= new Date(stengtTil) && todayDate >= new Date(stengtFra)) {
    return (
      <div>
        <div className={styles.tilgjengelighetsstatus}>
          <img src={StatusRod} alt="Rødt ikon som representerer at tiltaksgjennomføringen er stengt" />
          <div title={`Midlertidig stengt mellom ${formaterDato(stengtFra)} og ${formaterDato(stengtTil)}`}>Midlertidig stengt</div>
        </div>
      </div>
    );
  }
  if (status === 'Ledig' || !status) {
    return (
      <div>
        <div className={styles.tilgjengelighetsstatus}>
          <img src={StatusGronn} alt="Grønt ikon som representerer at tilgjengelighetsstatus er åpent" />
          <div>Åpent</div>
        </div>
      </div>
    );
  } else if (status === 'Stengt') {
    return (
      <div title={estimert_ventetid ?? ''}>
        <div className={styles.tilgjengelighetsstatus}>
          <img src={StatusRod} alt="Rødt ikon som representerer at tilgjengelighetsstatus er stengt" />
          <div>Stengt</div>
        </div>
        {estimert_ventetid ? <small className={styles.estimert_ventetid}>{estimert_ventetid}</small> : null}
      </div>
    );
  } else if (status === 'Venteliste') {
    return (
      <div title={estimert_ventetid ?? ''}>
        <div className={styles.tilgjengelighetsstatus}>
          <img src={StatusGul} alt="Gult ikon som representerer at tilgjengelighetsstatus er venteliste" />
          <div>Venteliste</div>
        </div>
        {estimert_ventetid ? <small className={styles.estimert_ventetid}>{estimert_ventetid}</small> : null}
      </div>
    );
  }

  return null;
}
