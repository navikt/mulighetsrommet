import { Alert, Button, Heading, Loader, Pagination } from '@navikt/ds-react';
import { useAtom } from 'jotai';
import { RESET } from 'jotai/utils';
import { useEffect } from 'react';
import { Tiltaksgjennomforing } from '../../core/api/models';
import { useHentBrukerdata } from '../../core/api/queries/useHentBrukerdata';
import useTiltaksgjennomforing from '../../core/api/queries/useTiltaksgjennomforing';
import { paginationAtom, tiltaksgjennomforingsfilter } from '../../core/atoms/atoms';
import { usePrepopulerFilter } from '../../hooks/usePrepopulerFilter';

import { Feilmelding, forsokPaNyttLink } from '../feilmelding/Feilmelding';
import { Gjennomforingsrad } from './Gjennomforingsrad';
import styles from './Tiltaksgjennomforingsoversikt.module.scss';

const Tiltaksgjennomforingsoversikt = () => {
  const [page, setPage] = useAtom(paginationAtom);
  const { forcePrepopulerFilter } = usePrepopulerFilter();
  const elementsPerPage = 15;
  const pagination = (tiltaksgjennomforing: Tiltaksgjennomforing[]) => {
    return Math.ceil(tiltaksgjennomforing.length / elementsPerPage);
  };
  const [_, setFilter] = useAtom(tiltaksgjennomforingsfilter);
  const brukerdata = useHentBrukerdata();

  const { data: tiltaksgjennomforinger = [], isLoading, isError, isFetching } = useTiltaksgjennomforing();

  useEffect(() => {
    if (tiltaksgjennomforinger.length <= elementsPerPage && !isFetching) {
      // Reset state
      setPage(1);
    }
  }, [tiltaksgjennomforinger]);

  if (isLoading || isFetching || brukerdata.isLoading || brukerdata.isFetching) {
    return <Loader size="xlarge" />;
  }

  if (isError) {
    return <Alert variant="error">Det har skjedd en feil</Alert>;
  }

  const gjennomforingerForSide = tiltaksgjennomforinger
    // TODO Sortering kommer i https://trello.com/c/gfsrx6nS/402-dropdown-for-sorteringsvalg
    // .sort((a, b) => {
    //   const sortOrDefault = sort || {
    //     orderBy: 'tiltakstypeNavn',
    //     direction: 'ascending',
    //   };

    //   const comparator = (a: any, b: any, orderBy: string | number) => {
    //     const compare = (item1: any, item2: any) => {
    //       if (item2 < item1 || item2 === undefined) {
    //         return -1;
    //       }
    //       if (item2 > item1) {
    //         return 1;
    //       }
    //       return 0;
    //     };
    //     if (orderBy === 'oppstart') {
    //       const dateB = b.oppstart === 'lopende' ? new Date() : new Date(b.oppstartsdato);
    //       const dateA = a.oppstart === 'lopende' ? new Date() : new Date(a.oppstartsdato);
    //       return compare(dateA, dateB);
    //     } else if (orderBy === 'tiltakstypeNavn') {
    //       return compare(a.tiltakstype.tiltakstypeNavn, b.tiltakstype.tiltakstypeNavn);
    //     } else {
    //       return compare(a[orderBy], b[orderBy]);
    //     }
    //   };
    //   return sortOrDefault.direction === 'ascending'
    //     ? comparator(b, a, sortOrDefault.orderBy)
    //     : comparator(a, b, sortOrDefault.orderBy);
    // })
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

  if (!brukerdata?.data?.innsatsgruppe) {
    return (
      <Feilmelding
        header={<>Kunne ikke hente brukers innsatsgruppe</>}
        beskrivelse={
          <>
            <>
              Vi kan ikke hente brukerens innsatsgruppe. Kontroller at brukeren er under oppfølging og finnes i Arena,
              og&nbsp;
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
    <div className="w-full flex flex-col gap-4">
      {tiltaksgjennomforinger.length > 0 ? (
        <Heading level="1" size="xsmall" data-testid="antall-tiltak-top">
          Viser {(page - 1) * elementsPerPage + 1}-{gjennomforingerForSide.length + (page - 1) * elementsPerPage} av{' '}
          {tiltaksgjennomforinger.length} tiltak
        </Heading>
      ) : null}
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
    </div>
  );
};

export default Tiltaksgjennomforingsoversikt;
