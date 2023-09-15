import classNames from 'classnames';
import { useAtom } from 'jotai';
import { TiltaksgjennomforingOppstartstype, VeilederflateTiltaksgjennomforing } from 'mulighetsrommet-api-client';
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

const visOppstartsdato = (oppstart: TiltaksgjennomforingOppstartstype, oppstartsdato?: string) => {
  switch (oppstart) {
    case TiltaksgjennomforingOppstartstype.FELLES:
      return formaterDato(oppstartsdato!);
    case TiltaksgjennomforingOppstartstype.LOPENDE:
      return 'Løpende oppstart';
  }
};

export function Gjennomforingsrad({ tiltaksgjennomforing, index }: Props) {
  const [page] = useAtom(paginationAtom);
  const {
    sanityId,
    navn,
    arrangor,
    tiltakstype,
    lokasjon,
    tilgjengelighet,
    oppstart,
    oppstartsdato,
    estimertVentetid,
    stengtFra,
    stengtTil,
  } = tiltaksgjennomforing;

  return (
    <li className={styles.list_element} id={`list_element_${index}`}>
      <Lenke to={`tiltak/${sanityId}#page=${page}`} data-testid="lenke_tiltaksgjennomforing">
        <div className={styles.gjennomforing_container}>
          <div className={classNames(styles.flex, styles.navn)}>
            <BodyShort size="small" title={navn} className={classNames(styles.truncate, styles.as_link)}>
              {navn}
            </BodyShort>
            <BodyShort size="small" title={arrangor?.selskapsnavn} className={styles.muted}>
              {arrangor?.selskapsnavn}
            </BodyShort>
          </div>
          <div className={classNames(styles.infogrid, styles.metadata)}>
            <BodyShort size="small" title={tiltakstype.navn} className={styles.truncate}>
              {tiltakstype.navn}
            </BodyShort>
            <BodyShort size="small" title={lokasjon} className={styles.truncate}>
              {lokasjon}
            </BodyShort>
            <BodyShort size="small" title={visOppstartsdato(oppstart, oppstartsdato)} className={styles.truncate}>
              {visOppstartsdato(oppstart, oppstartsdato)}
            </BodyShort>
            <TilgjengelighetsstatusComponent
              status={tilgjengelighet}
              estimertVentetid={estimertVentetid}
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
