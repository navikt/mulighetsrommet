import { Oppstart, Tilgjengelighetsstatus } from '../../core/api/models';
import StatusGronn from '../../ikoner/Sirkel-gronn.png';
import StatusGul from '../../ikoner/Sirkel-gul.png';
import StatusRod from '../../ikoner/Sirkel-rod.png';
import styles from './Tilgjengelighetsstatus.module.scss';

interface Props {
  oppstart: Oppstart;
  status?: Tilgjengelighetsstatus;
  estimert_ventetid?: string;
}

export function TilgjengelighetsstatusComponent({ oppstart, status, estimert_ventetid }: Props) {
  if (oppstart === 'midlertidig_stengt') {
    return (
      <div>
        <div className={styles.tilgjengelighetsstatus}>
          <img src={StatusRod} alt="Rødt sirkelikon" />
          <div>Midlertidig stengt</div>
        </div>
        {estimert_ventetid ? (
          <small title={estimert_ventetid} className={styles.estimert_ventetid}>
            {estimert_ventetid}
          </small>
        ) : null}
      </div>
    );
  }

  if (status === 'Ledig' || !status) {
    return (
      <div>
        <div className={styles.tilgjengelighetsstatus}>
          <img src={StatusGronn} alt="Tilgjengelighetsstatus åpent" />
          <div>Åpent</div>
        </div>
      </div>
    );
  } else if (status === 'Stengt') {
    return (
      <div title={estimert_ventetid ?? ''}>
        <div className={styles.tilgjengelighetsstatus}>
          <img src={StatusRod} alt="Tilgjengelighetsstatus stengt" />
          <div>Stengt</div>
        </div>
        {estimert_ventetid ? <small className={styles.estimert_ventetid}>{estimert_ventetid}</small> : null}
      </div>
    );
  } else if (status === 'Venteliste') {
    return (
      <div title={estimert_ventetid ?? ''}>
        <div className={styles.tilgjengelighetsstatus}>
          <img src={StatusGul} alt="Tilgjengelighetsstatus venteliste" />
          <div>Venteliste</div>
        </div>
        {estimert_ventetid ? <small className={styles.estimert_ventetid}>{estimert_ventetid}</small> : null}
      </div>
    );
  }

  return null;
}
