import { Alert, BodyShort, Button, Heading, Ingress, Loader, Pagination, Table } from '@navikt/ds-react';
import classNames from 'classnames';
import { useAtom } from 'jotai';
import { RESET } from 'jotai/utils';
import { useEffect, useState } from 'react';
import { logEvent } from '../../core/api/logger';
import { Oppstart, Tiltaksgjennomforing } from '../../core/api/models';
import { useHentBrukerdata } from '../../core/api/queries/useHentBrukerdata';
import useTiltaksgjennomforing from '../../core/api/queries/useTiltaksgjennomforing';
import { paginationAtom, tiltaksgjennomforingsfilter } from '../../core/atoms/atoms';
import { usePrepopulerFilter } from '../../hooks/usePrepopulerFilter';

import { Feilmelding, forsokPaNyttLink } from '../feilmelding/Feilmelding';
import Kopiknapp from '../kopiknapp/Kopiknapp';
import Lenke from '../lenke/Lenke';
import styles from './Tabell.module.scss';
import { TilgjengelighetsstatusComponent } from './Tilgjengelighetsstatus';

const TiltaksgjennomforingsTabell = () => {
  const [sort, setSort] = useState<any>();
  const [page, setPage] = useAtom(paginationAtom);
  const { forcePrepopulerFilter } = usePrepopulerFilter();
  const rowsPerPage = 15;
  const pagination = (tiltaksgjennomforing: Tiltaksgjennomforing[]) => {
    return Math.ceil(tiltaksgjennomforing.length / rowsPerPage);
  };
  const [filter, setFilter] = useAtom(tiltaksgjennomforingsfilter);
  const brukerdata = useHentBrukerdata();

  const { data: tiltaksgjennomforinger = [], isLoading, isError, isFetching } = useTiltaksgjennomforing();

  useEffect(() => {
    if (tiltaksgjennomforinger.length <= rowsPerPage && !isFetching) {
      // Reset state
      setPage(1);
    }
  }, [tiltaksgjennomforinger]);

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

  if (isLoading || isFetching || brukerdata.isLoading || brukerdata.isFetching) {
    return <Loader size="xlarge" />;
  }

  if (isError) {
    return <Alert variant="error">Det har skjedd en feil</Alert>;
  }

  const gjennomforingerForSide = tiltaksgjennomforinger
    .sort((a, b) => {
      const sortOrDefault = sort || {
        orderBy: 'tiltakstypeNavn',
        direction: 'ascending',
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
      return sortOrDefault.direction === 'ascending'
        ? comparator(b, a, sortOrDefault.orderBy)
        : comparator(a, b, sortOrDefault.orderBy);
    })
    .slice((page - 1) * rowsPerPage, page * rowsPerPage);

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

  if (tiltaksgjennomforinger.length == 0) {
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
      <Table
        zebraStripes
        size="small"
        sort={sort}
        data-testid="tabell_tiltakstyper"
        className={classNames(styles.tabell)}
        onSortChange={sortKey => {
          setSort(
            sort && sortKey === sort.orderBy && sort.direction === 'descending'
              ? undefined
              : {
                  orderBy: sortKey,
                  direction:
                    sort && sortKey === sort.orderBy && sort.direction === 'ascending' ? 'descending' : 'ascending',
                }
          );
          const directionForLogging = sort
            ? sortKey === sort.orderBy && sort.direction === 'ascending'
              ? 'descending'
              : 'neutral'
            : 'ascending';
          if (directionForLogging !== 'neutral')
            logEvent('mulighetsrommet.sortering', { sortKey }, { direction: directionForLogging });
        }}
      >
        <Table.Header>
          <Table.Row>
            <Table.ColumnHeader
              sortKey="tiltaksgjennomforingNavn"
              sortable
              className={styles.tabell_tiltaksnavn}
              data-testid="tabellheader_tiltaksnavn"
            >
              Tiltaksnavn
            </Table.ColumnHeader>
            <Table.ColumnHeader sortKey="tiltaksnummer" sortable data-testid="tabellheader_tiltaksnummer">
              Tiltaksnr.
            </Table.ColumnHeader>
            <Table.ColumnHeader sortKey="tiltakstypeNavn" sortable data-testid="tabellheader_tiltakstype">
              Tiltakstype
            </Table.ColumnHeader>
            <Table.ColumnHeader sortKey="lokasjon" sortable data-testid="tabellheader_lokasjon">
              Lokasjon
            </Table.ColumnHeader>
            <Table.ColumnHeader sortKey="oppstart" sortable data-testid="tabellheader_oppstartsdato">
              Oppstartsdato
            </Table.ColumnHeader>
            <Table.ColumnHeader sortKey="status" sortable data-testid="tabellheader_status">
              Status
            </Table.ColumnHeader>
          </Table.Row>
        </Table.Header>
        <Table.Body>
          {gjennomforingerForSide.map(
            ({
              _id,
              tiltaksnummer,
              tiltaksgjennomforingNavn,
              oppstart,
              estimert_ventetid,
              oppstartsdato,
              lokasjon,
              tiltakstype,
              kontaktinfoArrangor,
              tilgjengelighetsstatus,
            }) => (
              <Table.Row key={_id}>
                <Table.DataCell className={styles.tabell_tiltaksnavn}>
                  <Lenke
                    to={`tiltak/${_id}#filter=${encodeURIComponent(JSON.stringify(filter))}`}
                    isInline
                    data-testid="tabell_tiltaksgjennomforing"
                  >
                    {tiltaksgjennomforingNavn}
                  </Lenke>
                  <div>{kontaktinfoArrangor?.selskapsnavn}</div>
                </Table.DataCell>
                <Table.DataCell data-testid="tabell_tiltaksnummer" className={classNames(styles.tabell_tiltaksnummer)}>
                  {tiltaksnummer ? (
                    <div className={styles.tabell_wrapper}>
                      {tiltaksnummer}{' '}
                      <Kopiknapp kopitekst={tiltaksnummer?.toString()} dataTestId="tabell_knapp_kopier" />
                    </div>
                  ) : null}
                </Table.DataCell>
                <Table.DataCell>{tiltakstype.tiltakstypeNavn}</Table.DataCell>
                <Table.DataCell>{lokasjon}</Table.DataCell>
                <Table.DataCell>{visOppstartsdato(oppstart, oppstartsdato)}</Table.DataCell>
                <Table.DataCell>
                  <TilgjengelighetsstatusComponent
                    oppstart={oppstart}
                    status={tilgjengelighetsstatus}
                    estimert_ventetid={estimert_ventetid}
                  />
                </Table.DataCell>
              </Table.Row>
            )
          )}
        </Table.Body>
      </Table>
      <div className={styles.under_tabell}>
        {tiltaksgjennomforinger.length > 0 ? (
          <>
            <Heading level="1" size="xsmall" data-testid="antall-tiltak">
              Viser {(page - 1) * rowsPerPage + 1}-{gjennomforingerForSide.length + (page - 1) * rowsPerPage} av{' '}
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

export default TiltaksgjennomforingsTabell;
