import { Alert, BodyShort, Button, Loader, Pagination } from '@navikt/ds-react';
import { useAtom } from 'jotai';
import { RESET } from 'jotai/utils';
import { useEffect, useRef, useState } from 'react';
import { useHentBrukerdata } from '../../core/api/queries/useHentBrukerdata';
import useTiltaksgjennomforinger from '../../core/api/queries/useTiltaksgjennomforinger';
import { paginationAtom, tiltaksgjennomforingsfilter } from '../../core/atoms/atoms';
import { usePrepopulerFilter } from '../../hooks/usePrepopulerFilter';
import { ApiError, SanityTiltaksgjennomforing } from 'mulighetsrommet-api-client';
import { logEvent } from '../../core/api/logger';
import { Feilmelding, forsokPaNyttLink } from '../feilmelding/Feilmelding';
import { Sorteringsmeny } from '../sorteringmeny/Sorteringsmeny';
import { Gjennomforingsrad } from './Gjennomforingsrad';
import styles from './Tiltaksgjennomforingsoversikt.module.scss';
import Lenke from '../lenke/Lenke';
import { porten } from 'mulighetsrommet-frontend-common/constants';

const Tiltaksgjennomforingsoversikt = () => {
  const [page, setPage] = useAtom(paginationAtom);
  const { forcePrepopulerFilter } = usePrepopulerFilter();
  const elementsPerPage = 15;
  const pagination = (tiltaksgjennomforing: SanityTiltaksgjennomforing[]) => {
    return Math.ceil(tiltaksgjennomforing.length / elementsPerPage);
  };
  const [, setFilter] = useAtom(tiltaksgjennomforingsfilter);
  const brukerdata = useHentBrukerdata();

  const { data: tiltaksgjennomforinger = [], isLoading, isError, error, isFetching } = useTiltaksgjennomforinger();
  const [sortValue, setSortValue] = useState<string>('tiltakstypeNavn-ascending');
  const didMountRef = useRef(false);

  useEffect(() => {
    if (tiltaksgjennomforinger.length <= elementsPerPage && !isFetching) {
      // Reset state
      setPage(1);
    }
  }, [tiltaksgjennomforinger]);

  useEffect(() => {
    //sørger for at vi ikke logger metrikker for første render
    if (didMountRef.current) logEvent('mulighetsrommet.sortering', { value: sortValue });
    didMountRef.current = true;
  }, [sortValue]);

  if (tiltaksgjennomforinger.length === 0 && (isLoading || brukerdata.isLoading)) {
    return (
      <div className={styles.filter_loader}>
        <Loader size="xlarge" />
      </div>
    );
  }

  if (isError) {
    if (error instanceof ApiError) {
      return (
        <Alert variant="error">
          Det har dessverre skjedd en feil. Om feilen gjentar seg, ta kontakt i{' '}
          {
            <Lenke to={porten} target={'_blank'}>
              Porten
            </Lenke>
          }
          <pre>{JSON.stringify({ message: error.message, status: error.status, url: error.url }, null, 2)}</pre>
        </Alert>
      );
    } else {
      return (
        <Alert variant="error">
          Det har dessverre skjedd en feil. Om feilen gjentar seg, ta kontakt i{' '}
          {
            <Lenke to={porten} target={'_blank'}>
              Porten
            </Lenke>
          }
          .
        </Alert>
      );
    }
  }

  const getSort = (sortValue: string) => {
    return {
      orderBy: sortValue.split('-')[0],
      direction: sortValue.split('-')[1],
    };
  };

  const sorter = (
    tiltaksgjennomforinger: SanityTiltaksgjennomforing[],
    forceOrder: 'ascending' | 'descending' = 'ascending'
  ): SanityTiltaksgjennomforing[] => {
    return tiltaksgjennomforinger.sort((a, b) => {
      const sort = getSort(sortValue);
      const comparator = (a: any, b: any, orderBy: string | number) => {
        const compare = (item1: any, item2: any) => {
          if (item2 < item1 || item2 === undefined) return -1;
          if (item2 > item1) return 1;
          return 0;
        };

        if (orderBy === 'oppstart') {
          const dateB = b.oppstart === 'lopende' ? new Date() : new Date(b.oppstartsdato);
          const dateA = a.oppstart === 'lopende' ? new Date() : new Date(a.oppstartsdato);
          return forceOrder === 'ascending' ? compare(dateA, dateB) : compare(dateB, dateA);
        } else if (orderBy === 'tiltakstypeNavn') {
          return compare(a.tiltakstype.tiltakstypeNavn, b.tiltakstype.tiltakstypeNavn);
        } else {
          return compare(a[orderBy], b[orderBy]);
        }
      };

      return sort.direction === 'ascending' ? comparator(b, a, sort.orderBy) : comparator(a, b, sort.orderBy);
    });
  };

  const lopendeOppstartForst = (
    lopendeGjennomforinger: SanityTiltaksgjennomforing[],
    gjennomforingerMedOppstartIFremtiden: SanityTiltaksgjennomforing[],
    gjennomforingerMedOppstartHarVaert: SanityTiltaksgjennomforing[]
  ): SanityTiltaksgjennomforing[] => {
    return [
      ...lopendeGjennomforinger,
      ...sorter(gjennomforingerMedOppstartIFremtiden),
      ...sorter(gjennomforingerMedOppstartHarVaert, 'descending'),
    ];
  };

  const lopendeGjennomforinger = tiltaksgjennomforinger.filter(gj => gj.oppstart === 'lopende');
  const gjennomforingerMedOppstartIFremtiden = tiltaksgjennomforinger.filter(
    gj => gj.oppstart !== 'lopende' && new Date(gj.oppstartsdato!!) >= new Date()
  );
  const gjennomforingerMedOppstartHarVaert = tiltaksgjennomforinger.filter(
    gj => gj.oppstart !== 'lopende' && new Date(gj.oppstartsdato!!) <= new Date()
  );

  const gjennomforingerForSide = (
    getSort(sortValue).orderBy === 'oppstart'
      ? lopendeOppstartForst(
          lopendeGjennomforinger,
          gjennomforingerMedOppstartIFremtiden,
          gjennomforingerMedOppstartHarVaert
        )
      : sorter(tiltaksgjennomforinger)
  ).slice((page - 1) * elementsPerPage, page * elementsPerPage);

  if (!brukerdata?.data?.geografiskEnhet) {
    return (
      <Feilmelding
        header="Kunne ikke hente brukers geografiske enhet"
        beskrivelse={
          <>
            Brukers geografiske enhet kunne ikke hentes. Kontroller at brukeren er under oppfølging og finnes i Arena,
            og {forsokPaNyttLink()}
          </>
        }
        ikonvariant="error"
      />
    );
  }

  if (!brukerdata?.data?.innsatsgruppe && !brukerdata?.data?.servicegruppe) {
    return (
      <Feilmelding
        header="Kunne ikke hente brukers innsatsgruppe eller servicegruppe"
        beskrivelse={
          <>
            Vi kan ikke hente brukerens innsatsgruppe eller servicegruppe. Kontroller at brukeren er under oppfølging og
            finnes i Arena, og <br /> {forsokPaNyttLink()}
          </>
        }
        ikonvariant="error"
      />
    );
  }

  if (tiltaksgjennomforinger.length === 0) {
    return (
      <Feilmelding
        header="Ingen tiltaksgjennomføringer funnet"
        beskrivelse="Prøv å justere søket eller filteret for å finne det du leter etter"
        ikonvariant="warning"
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
          <BodyShort data-testid="antall-tiltak-top">
            Viser {(page - 1) * elementsPerPage + 1}-{gjennomforingerForSide.length + (page - 1) * elementsPerPage} av{' '}
            {tiltaksgjennomforinger.length} tiltak
          </BodyShort>
        ) : null}
        <Sorteringsmeny sortValue={sortValue} setSortValue={setSortValue} />
      </div>
      <ul className={styles.gjennomforinger} data-testid="oversikt_tiltaksgjennomforinger">
        {gjennomforingerForSide.map((gjennomforing, index) => {
          return <Gjennomforingsrad key={gjennomforing._id} index={index} tiltaksgjennomforing={gjennomforing} />;
        })}
      </ul>
      <div className={styles.under_oversikt}>
        {tiltaksgjennomforinger.length > 0 ? (
          <>
            <BodyShort data-testid="antall-tiltak">
              Viser {(page - 1) * elementsPerPage + 1}-{gjennomforingerForSide.length + (page - 1) * elementsPerPage} av{' '}
              {tiltaksgjennomforinger.length} tiltak
            </BodyShort>
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
