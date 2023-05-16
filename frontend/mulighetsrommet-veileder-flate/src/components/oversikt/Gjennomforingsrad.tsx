/* eslint-disable camelcase */
import classNames from 'classnames';
import { useAtom } from 'jotai';
import { SanityTiltaksgjennomforing } from 'mulighetsrommet-api-client';
import { paginationAtom, tiltaksgjennomforingsfilter } from '../../core/atoms/atoms';
import { formaterDato } from '../../utils/Utils';
import Lenke from '../lenke/Lenke';
import styles from './Gjennomforingsrad.module.scss';
import { TilgjengelighetsstatusComponent } from './Tilgjengelighetsstatus';
import { BodyShort } from '@navikt/ds-react';
import { ChevronRightIcon } from '@navikt/aksel-icons';

interface Props {
  tiltaksgjennomforing: SanityTiltaksgjennomforing;
  index: number;
}

const visOppstartsdato = (oppstart: SanityTiltaksgjennomforing.oppstart, oppstartsdato?: string) => {
  switch (oppstart) {
    case 'dato':
      return formaterDato(oppstartsdato!);
    case 'lopende':
      return 'Løpende oppstart';
  }
};

export function Gjennomforingsrad({ tiltaksgjennomforing, index }: Props) {
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
    <li className={styles.list_element} id={`list_element_${index}`}>
      <Lenke
        to={`tiltak/${_id}#filter=${encodeURIComponent(JSON.stringify(filter))}&page=${page}`}
        data-testid="lenke_tiltaksgjennomforing"
      >
        <div className={styles.gjennomforing_container}>
          <div className={classNames(styles.flex, styles.navn)}>
            <BodyShort
              size="small"
              title={tiltaksgjennomforingNavn}
              className={classNames(styles.truncate, styles.as_link)}
            >
              {tiltaksgjennomforingNavn}
            </BodyShort>
            <BodyShort size="small" title={kontaktinfoArrangor?.selskapsnavn} className={styles.muted}>
              {kontaktinfoArrangor?.selskapsnavn}
            </BodyShort>
          </div>
          <div className={classNames(styles.infogrid, styles.metadata)}>
            <BodyShort size="small" title={tiltakstype.tiltakstypeNavn} className={styles.truncate}>
              {tiltakstype.tiltakstypeNavn}
            </BodyShort>
            <BodyShort size="small" title={lokasjon} className={styles.truncate}>
              {lokasjon}
            </BodyShort>
            <BodyShort size="small" title={visOppstartsdato(oppstart, oppstartsdato)} className={styles.truncate}>
              {visOppstartsdato(oppstart, oppstartsdato)}
            </BodyShort>
            <TilgjengelighetsstatusComponent
              oppstart={oppstart}
              status={tilgjengelighetsstatus}
              estimert_ventetid={estimert_ventetid}
            />
          </div>
          <ChevronRightIcon className={styles.ikon} />
        </div>
      </Lenke>
    </li>
  );
}
