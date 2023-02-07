/* eslint-disable camelcase */
import classNames from 'classnames';
import { useAtom } from 'jotai';
import { Oppstart, Tiltaksgjennomforing } from '../../core/api/models';
import { paginationAtom, tiltaksgjennomforingsfilter } from '../../core/atoms/atoms';
import Lenke from '../lenke/Lenke';
import styles from './Gjennomforingsrad.module.scss';
import { TilgjengelighetsstatusComponent } from './Tilgjengelighetsstatus';
import { Next } from '@navikt/ds-icons';
import { formaterDato } from '../../utils/Utils';

interface Props {
  tiltaksgjennomforing: Tiltaksgjennomforing;
}

const visOppstartsdato = (oppstart: Oppstart, oppstartsdato?: string) => {
  switch (oppstart) {
    case 'dato':
      return formaterDato(oppstartsdato!);
    case 'lopende':
      return 'Løpende oppstart';
    case 'midlertidig_stengt':
      return 'Midlertidig stengt';
  }
};

export function Gjennomforingsrad({ tiltaksgjennomforing }: Props) {
  const [filter] = useAtom(tiltaksgjennomforingsfilter);
  const [page] = useAtom(paginationAtom);
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
      <Lenke
        to={`tiltak/${_id}#filter=${encodeURIComponent(JSON.stringify(filter))}&page=${page}`}
        data-testid="lenke_tiltaksgjennomforing"
      >
        <div className={styles.gjennomforing_container}>
          <div className={classNames(styles.flex, styles.navn)}>
            <span title={tiltaksgjennomforingNavn} className={classNames(styles.truncate, styles.as_link)}>
              {tiltaksgjennomforingNavn}
            </span>
            <span title={kontaktinfoArrangor?.selskapsnavn} className={styles.muted}>
              {kontaktinfoArrangor?.selskapsnavn}
            </span>
          </div>
          <div className={classNames(styles.infogrid, styles.metadata)}>
            <span title={tiltakstype.tiltakstypeNavn} className={styles.truncate}>
              {tiltakstype.tiltakstypeNavn}
            </span>
            <span title={lokasjon} className={styles.truncate}>
              {lokasjon}
            </span>
            <span title={visOppstartsdato(oppstart, oppstartsdato)} className={styles.truncate}>
              {visOppstartsdato(oppstart, oppstartsdato)}
            </span>
            <TilgjengelighetsstatusComponent
              oppstart={oppstart}
              status={tilgjengelighetsstatus}
              estimert_ventetid={estimert_ventetid}
            />
          </div>
          <div className={classNames(styles.as_link, styles.ikon)}>
            <Next />
          </div>
        </div>
      </Lenke>
    </li>
  );
}
