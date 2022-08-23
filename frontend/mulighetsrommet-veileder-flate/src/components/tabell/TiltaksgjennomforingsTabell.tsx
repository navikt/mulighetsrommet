import React, { useEffect, useState } from 'react';
import { Alert, BodyShort, Button, Heading, Ingress, Loader, Pagination, Table } from '@navikt/ds-react';
import './Tabell.less';
import { useAtom } from 'jotai';
import Lenke from '../lenke/Lenke';
import Kopiknapp from '../kopiknapp/Kopiknapp';
import StatusGronn from '../../ikoner/Sirkel-gronn.png';
import StatusGul from '../../ikoner/Sirkel-gul.png';
import StatusRod from '../../ikoner/Sirkel-rod.png';
import useTiltaksgjennomforing from '../../core/api/queries/useTiltaksgjennomforing';
import { logEvent } from '../../core/api/logger';
import { Tilgjengelighetsstatus, Tiltaksgjennomforing } from '../../core/api/models';
import { paginationAtom, tiltaksgjennomforingsfilter } from '../../core/atoms/atoms';
import { RESET } from 'jotai/utils';
import { Feilmelding } from '../feilmelding/Feilmelding';
import { usePrepopulerFilter } from '../../hooks/usePrepopulerFilter';

const TiltaksgjennomforingsTabell = () => {
  const [sort, setSort] = useState<any>();
  const [page, setPage] = useAtom(paginationAtom);
  const { forcePrepopulerFilter } = usePrepopulerFilter();
  const rowsPerPage = 15;
  const pagination = (tiltaksgjennomforing: Tiltaksgjennomforing[]) => {
    return Math.ceil(tiltaksgjennomforing.length / rowsPerPage);
  };
  const [filter, setFilter] = useAtom(tiltaksgjennomforingsfilter);

  const { data: tiltaksgjennomforinger = [], isLoading, isError, isFetching } = useTiltaksgjennomforing();

  useEffect(() => {
    if (tiltaksgjennomforinger.length <= rowsPerPage && !isFetching) {
      // Reset state
      setPage(1);
    }
  }, [tiltaksgjennomforinger]);

  const visStatus = (status?: Tilgjengelighetsstatus) => {
    if (status === 'Apent' || !status) {
      return (
        <div className="tabell__tilgjengelighetsstatus">
          <img src={StatusGronn} alt="Grønt sirkelikon" />
          <div>Åpent</div>
        </div>
      );
    } else if (status === 'Stengt') {
      return (
        <div className="tabell__tilgjengelighetsstatus">
          <img src={StatusRod} alt="Rødt sirkelikon" />
          <div>Stengt</div>
        </div>
      );
    } else if (status === 'Venteliste') {
      return (
        <div className="tabell__tilgjengelighetsstatus">
          <img src={StatusGul} alt="Gult sirkelikon" />
          <div>Venteliste</div>
        </div>
      );
    }
  };

  if (isLoading || isFetching) {
    return <Loader className="filter-loader" size="xlarge" />;
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

  if (tiltaksgjennomforinger.length === 0) {
    return (
      <Feilmelding>
        <Ingress>Ingen tiltaksgjennomføringer funnet</Ingress>
        <BodyShort>Prøv å justere søket eller filteret for å finne det du leter etter</BodyShort>
        <Button
          variant="tertiary"
          onClick={() => {
            setFilter(RESET);
            forcePrepopulerFilter(true);
          }}
        >
          Tilbakestill filter
        </Button>
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
        className="tabell"
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
              className="tabell__kolonne__tiltaksnavn"
              data-testid="tabellheader_tiltaksnavn"
            >
              Tiltaksnavn
            </Table.ColumnHeader>
            <Table.ColumnHeader
              sortKey="tiltaksnummer"
              sortable
              className="tabell__kolonne__tiltaksnummer"
              data-testid="tabellheader_tiltaksnummer"
            >
              Tiltaksnr.
            </Table.ColumnHeader>
            <Table.ColumnHeader
              sortKey="tiltakstypeNavn"
              sortable
              className="tabell__kolonne__tiltakstype"
              data-testid="tabellheader_tiltakstype"
            >
              Tiltakstype
            </Table.ColumnHeader>
            <Table.ColumnHeader
              sortKey="lokasjon"
              sortable
              className="tabell__kolonne__oppstart"
              data-testid="tabellheader_lokasjon"
            >
              Lokasjon
            </Table.ColumnHeader>
            <Table.ColumnHeader
              sortKey="oppstart"
              sortable
              className="tabell__kolonne__oppstart"
              data-testid="tabellheader_oppstartsdato"
            >
              Oppstartsdato
            </Table.ColumnHeader>
            <Table.ColumnHeader
              sortKey="status"
              sortable
              className="tabell__kolonne__plasser"
              data-testid="tabellheader_status"
            >
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
              oppstartsdato,
              lokasjon,
              tiltakstype,
              kontaktinfoArrangor,
              tilgjengelighetsstatus,
            }) => (
              <Table.Row key={_id}>
                <Table.DataCell className="tabell__tiltaksnavn">
                  <Lenke
                    to={`${tiltaksnummer}#filter=${encodeURIComponent(JSON.stringify(filter))}`}
                    isInline
                    data-testid="tabell_tiltaksgjennomforing"
                  >
                    {tiltaksgjennomforingNavn}
                  </Lenke>
                  <div>{kontaktinfoArrangor.selskapsnavn}</div>
                </Table.DataCell>
                <Table.DataCell data-testid="tabell_tiltaksnummer">
                  <div className="tabell__tiltaksnummer">
                    {tiltaksnummer}
                    <Kopiknapp kopitekst={tiltaksnummer!.toString()} dataTestId="tabell_knapp_kopier" />
                  </div>
                </Table.DataCell>
                <Table.DataCell>{tiltakstype.tiltakstypeNavn}</Table.DataCell>
                <Table.DataCell>{lokasjon}</Table.DataCell>
                <Table.DataCell>
                  {oppstart === 'dato'
                    ? new Date(oppstartsdato!).toLocaleString('no-NO', {
                        year: 'numeric',
                        month: '2-digit',
                        day: '2-digit',
                      })
                    : 'Løpende'}
                </Table.DataCell>
                <Table.DataCell>{visStatus(tilgjengelighetsstatus)}</Table.DataCell>
              </Table.Row>
            )
          )}
        </Table.Body>
      </Table>
      <div className="under-tabell">
        {tiltaksgjennomforinger.length > 0 ? (
          <>
            <Heading level="1" size="xsmall" data-testid="antall-tiltak">
              Viser {(page - 1) * rowsPerPage + 1}-{gjennomforingerForSide.length + (page - 1) * rowsPerPage} av{' '}
              {tiltaksgjennomforinger.length} tiltak
            </Heading>
            <Pagination
              data-testid="paginering"
              page={page}
              onPageChange={setPage}
              count={pagination(tiltaksgjennomforinger) === 0 ? 1 : pagination(tiltaksgjennomforinger)}
            />
          </>
        ) : null}
      </div>
    </div>
  );
};

export default TiltaksgjennomforingsTabell;
