import { useAtom } from 'jotai';
import { Oppstart, Tiltaksgjennomforing } from '../../core/api/models';
import { tiltaksgjennomforingsfilter } from '../../core/atoms/atoms';
import Lenke from '../lenke/Lenke';
import styles from './Gjennomforingsrad.module.scss';
import { TilgjengelighetsstatusComponent } from './Tilgjengelighetsstatus';

interface Props {
  tiltaksgjennomforing: Tiltaksgjennomforing;
}

const visOppstartsdato = (oppstart: Oppstart, oppstartsdato?: string) => {
  switch (oppstart) {
    case 'dato':
      return new Date(oppstartsdato!).toLocaleString('no-NO', {
        year: 'numeric',
        month: '2-digit',
        day: '2-digit',
      });
    case 'lopende':
      return 'Løpende';
    case 'midlertidig_stengt':
      return 'Midlertidig stengt';
  }
};

export function Gjennomforingsrad({ tiltaksgjennomforing }: Props) {
  const [filter] = useAtom(tiltaksgjennomforingsfilter);
  const {
    _id,
    tiltaksgjennomforingNavn,
    kontaktinfoArrangor,
    tiltakstype,
    lokasjon,
    tilgjengelighetsstatus,
    oppstart,
    oppstartsdato,
    estimert_ventetid,
  } = tiltaksgjennomforing;
  return (
    <li className={styles.list_element}>
      <div className={styles.gjennomforingContainer}>
        <div className={styles.flex}>
          <Lenke
            to={`tiltak/${_id}#filter=${encodeURIComponent(JSON.stringify(filter))}`}
            isInline
            data-testid="lenke_tiltaksgjennomforing"
          >
            {tiltaksgjennomforingNavn}
          </Lenke>
          <span className={styles.muted}>{kontaktinfoArrangor?.selskapsnavn}</span>
        </div>

        <span>{tiltakstype.tiltakstypeNavn}</span>
        <span>{lokasjon}</span>
        <span>{visOppstartsdato(oppstart, oppstartsdato)}</span>
        <TilgjengelighetsstatusComponent
          oppstart={oppstart}
          status={tilgjengelighetsstatus}
          estimert_ventetid={estimert_ventetid}
        />
      </div>
    </li>
  );
}
