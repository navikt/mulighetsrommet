import { Alert, Button, Heading, Loader, Pagination } from '@navikt/ds-react';
import { useAtom } from 'jotai';
import { RESET } from 'jotai/utils';
import { useEffect, useState } from 'react';
import { Tiltaksgjennomforing } from '../../core/api/models';
import { useHentBrukerdata } from '../../core/api/queries/useHentBrukerdata';
import useTiltaksgjennomforinger from '../../core/api/queries/useTiltaksgjennomforinger';
import { paginationAtom, tiltaksgjennomforingsfilter } from '../../core/atoms/atoms';
import { usePrepopulerFilter } from '../../hooks/usePrepopulerFilter';

import { Feilmelding, forsokPaNyttLink } from '../feilmelding/Feilmelding';
import { Gjennomforingsrad } from './Gjennomforingsrad';
import styles from './Tiltaksgjennomforingsoversikt.module.scss';
import { Sorteringsmeny } from '../sorteringmeny/Sorteringsmeny';

const Tiltaksgjennomforingsoversikt = () => {
  const [page, setPage] = useAtom(paginationAtom);
  const { forcePrepopulerFilter } = usePrepopulerFilter();
  const elementsPerPage = 15;
  const pagination = (tiltaksgjennomforing: Tiltaksgjennomforing[]) => {
    return Math.ceil(tiltaksgjennomforing.length / elementsPerPage);
  };
  const [, setFilter] = useAtom(tiltaksgjennomforingsfilter);
  const brukerdata = useHentBrukerdata();

  const { data: tiltaksgjennomforinger = [], isLoading, isError, isFetching } = useTiltaksgjennomforinger();
  const [sortValue, setSortValue] = useState<string>('tiltakstypeNavn-ascending');

  useEffect(() => {
    if (tiltaksgjennomforinger.length <= elementsPerPage && !isFetching) {
      // Reset state
      setPage(1);
    }
  }, [tiltaksgjennomforinger]);

  if (isLoading || isFetching || brukerdata.isLoading || brukerdata.isFetching) {
    return (
      <div className={styles.filter_loader}>
        <Loader size="xlarge" />
      </div>
    );
  }

  if (isError) {
    return <Alert variant="error">Det har skjedd en feil</Alert>;
  }

  const gjennomforingerForSide = tiltaksgjennomforinger
    .sort((a, b) => {
      const sort = {
        orderBy: sortValue.split('-')[0],
        direction: sortValue.split('-')[1],
      };

      const comparator = (a: any, b: any, orderBy: string | number) => {
        const compare = (item1: any, item2: any) => {
          if (item2 < item1 || item2 === undefined) {
            return -1;
          }
          if (item2 > item1) {
            return 1;
          }
          return 0;
        };
        if (orderBy === 'oppstart') {
          const dateB = b.oppstart === 'lopende' ? new Date() : new Date(b.oppstartsdato);
          const dateA = a.oppstart === 'lopende' ? new Date() : new Date(a.oppstartsdato);
          return compare(dateA, dateB);
        } else if (orderBy === 'tiltakstypeNavn') {
          return compare(a.tiltakstype.tiltakstypeNavn, b.tiltakstype.tiltakstypeNavn);
        } else {
          return compare(a[orderBy], b[orderBy]);
        }
      };
      return sort.direction === 'ascending' ? comparator(b, a, sort.orderBy) : comparator(a, b, sort.orderBy);
    })
    .slice((page - 1) * elementsPerPage, page * elementsPerPage);

  if (!brukerdata?.data?.oppfolgingsenhet) {
    return (
      <Feilmelding
        header={<>Kunne ikke hente brukers oppfølgingsenhet</>}
        beskrivelse={
          <>
            <>
              Brukers oppfølgingsenhet kunne ikke hentes. Kontroller at brukeren er under oppfølging og finnes i Arena,
              og&nbsp;
            </>
            {forsokPaNyttLink()}
          </>
        }
        ikonvariant="error"
      />
    );
  }

  if (!brukerdata?.data?.innsatsgruppe && !brukerdata?.data?.servicegruppe) {
    return (
      <Feilmelding
        header={<>Kunne ikke hente brukers innsatsgruppe eller servicegruppe</>}
        beskrivelse={
          <>
            <>
              Vi kan ikke hente brukerens innsatsgruppe eller servicegruppe. Kontroller at brukeren er under oppfølging
              og finnes i Arena, og&nbsp;
            </>
            {forsokPaNyttLink()}
          </>
        }
        ikonvariant="error"
      />
    );
  }

  if (tiltaksgjennomforinger.length === 0) {
    return (
      <Feilmelding
        ikonvariant="warning"
        header={<>Ingen tiltaksgjennomføringer funnet</>}
        beskrivelse={<>Prøv å justere søket eller filteret for å finne det du leter etter</>}
      >
        <>
          <Button
            variant="tertiary"
            onClick={() => {
              setFilter(RESET);
              forcePrepopulerFilter(true);
            }}
          >
            Tilbakestill filter
          </Button>
        </>
      </Feilmelding>
    );
  }

  return (
    <>
      <div className={styles.overskrift_og_sorteringsmeny}>
        {tiltaksgjennomforinger.length > 0 ? (
          <Heading level="1" size="xsmall" data-testid="antall-tiltak-top">
            Viser {(page - 1) * elementsPerPage + 1}-{gjennomforingerForSide.length + (page - 1) * elementsPerPage} av{' '}
            {tiltaksgjennomforinger.length} tiltak
          </Heading>
        ) : null}
        <Sorteringsmeny sortValue={sortValue} setSortValue={setSortValue} />
      </div>
      <ul className={styles.gjennomforinger} data-testid="oversikt_tiltaksgjennomforinger">
        {gjennomforingerForSide.map(gjennomforing => {
          return <Gjennomforingsrad key={gjennomforing._id} tiltaksgjennomforing={gjennomforing} />;
        })}
      </ul>
      <div className={styles.under_oversikt}>
        {tiltaksgjennomforinger.length > 0 ? (
          <>
            <Heading level="1" size="xsmall" data-testid="antall-tiltak">
              Viser {(page - 1) * elementsPerPage + 1}-{gjennomforingerForSide.length + (page - 1) * elementsPerPage} av{' '}
              {tiltaksgjennomforinger.length} tiltak
            </Heading>
            <Pagination
              size="small"
              data-testid="paginering"
              page={page}
              onPageChange={setPage}
              count={pagination(tiltaksgjennomforinger) === 0 ? 1 : pagination(tiltaksgjennomforinger)}
              data-version="v1"
            />
          </>
        ) : null}
      </div>
    </>
  );
};

export default Tiltaksgjennomforingsoversikt;
