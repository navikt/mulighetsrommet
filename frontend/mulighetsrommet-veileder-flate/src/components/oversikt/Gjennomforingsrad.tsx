/* eslint-disable camelcase */
import classNames from 'classnames';
import { useAtom } from 'jotai';
import { VeilederflateTiltaksgjennomforing } from 'mulighetsrommet-api-client';
import { paginationAtom } from '../../core/atoms/atoms';
import { formaterDato } from '../../utils/Utils';
import Lenke from '../lenke/Lenke';
import styles from './Gjennomforingsrad.module.scss';
import { TilgjengelighetsstatusComponent } from './Tilgjengelighetsstatus';
import { BodyShort } from '@navikt/ds-react';
import { ChevronRightIcon } from '@navikt/aksel-icons';

interface Props {
  tiltaksgjennomforing: VeilederflateTiltaksgjennomforing;
  index: number;
}

const visOppstartsdato = (oppstart: VeilederflateTiltaksgjennomforing.oppstart, oppstartsdato?: string) => {
  switch (oppstart) {
    case 'dato':
      return formaterDato(oppstartsdato!);
    case 'lopende':
      return 'Løpende oppstart';
  }
};

export function Gjennomforingsrad({ tiltaksgjennomforing, index }: Props) {
  const [page] = useAtom(paginationAtom);
  const {
    _id,
    tiltaksgjennomforingNavn,
    arrangor,
    tiltakstype,
    lokasjon,
    tilgjengelighetsstatus,
    oppstart,
    oppstartsdato,
    estimert_ventetid,
    stengtFra,
    stengtTil,
  } = tiltaksgjennomforing;

  return (
    <li className={styles.list_element} id={`list_element_${index}`}>
      <Lenke to={`tiltak/${_id}#page=${page}`} data-testid="lenke_tiltaksgjennomforing">
        <div className={styles.gjennomforing_container}>
          <div className={classNames(styles.flex, styles.navn)}>
            <BodyShort
              size="small"
              title={tiltaksgjennomforingNavn}
              className={classNames(styles.truncate, styles.as_link)}
            >
              {tiltaksgjennomforingNavn}
            </BodyShort>
            <BodyShort size="small" title={arrangor?.selskapsnavn} className={styles.muted}>
              {arrangor?.selskapsnavn}
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
              status={tilgjengelighetsstatus}
              estimert_ventetid={estimert_ventetid}
              stengtFra={stengtFra}
              stengtTil={stengtTil}
            />
          </div>
          <ChevronRightIcon className={styles.ikon} />
        </div>
      </Lenke>
    </li>
  );
}
