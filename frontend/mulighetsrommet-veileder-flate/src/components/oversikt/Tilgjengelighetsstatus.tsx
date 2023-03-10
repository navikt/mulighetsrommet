/* eslint-disable camelcase */
import { SanityTiltaksgjennomforing } from 'mulighetsrommet-api-client';
import { useFeatureToggles, VIS_TILGJENGELIGHETSSTATUS } from '../../core/api/feature-toggles';
import StatusGronn from '../../ikoner/Sirkel-gronn.png';
import StatusGul from '../../ikoner/Sirkel-gul.png';
import StatusRod from '../../ikoner/Sirkel-rod.png';
import styles from './Tilgjengelighetsstatus.module.scss';

interface Props {
  oppstart?: SanityTiltaksgjennomforing.oppstart;
  status?: SanityTiltaksgjennomforing.tilgjengelighetsstatus;
  estimert_ventetid?: string;
}

export function TilgjengelighetsstatusComponent({ oppstart, status, estimert_ventetid }: Props) {
  const { data } = useFeatureToggles();

  if (!data?.[VIS_TILGJENGELIGHETSSTATUS]) {
    return null;
  }

  if (oppstart === 'midlertidig_stengt') {
    return (
      <div>
        <div className={styles.tilgjengelighetsstatus}>
          <img src={StatusRod} alt="Rødt ikon som representerer at tilgjenglighetsstatus er midlertidig stengt" />
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
          <img src={StatusGronn} alt="Grønt ikon som representerer at tilgjenglighetsstatus er åpent" />
          <div>Åpent</div>
        </div>
      </div>
    );
  } else if (status === 'Stengt') {
    return (
      <div title={estimert_ventetid ?? ''}>
        <div className={styles.tilgjengelighetsstatus}>
          <img src={StatusRod} alt="Rødt ikon som representerer at tilgjenglighetsstatus er stengt" />
          <div>Stengt</div>
        </div>
        {estimert_ventetid ? <small className={styles.estimert_ventetid}>{estimert_ventetid}</small> : null}
      </div>
    );
  } else if (status === 'Venteliste') {
    return (
      <div title={estimert_ventetid ?? ''}>
        <div className={styles.tilgjengelighetsstatus}>
          <img src={StatusGul} alt="Gult ikon som representerer at tilgjenglighetsstatus er venteliste" />
          <div>Venteliste</div>
        </div>
        {estimert_ventetid ? <small className={styles.estimert_ventetid}>{estimert_ventetid}</small> : null}
      </div>
    );
  }

  return null;
}
