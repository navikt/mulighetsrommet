import React, { useState } from 'react';
import { Pagination, Table, Alert, Heading } from '@navikt/ds-react';
import './Tabell.less';
import { Tiltaksgjennomforing } from '../../../../mulighetsrommet-api-client';
import Lenke from '../lenke/Lenke';
import Kopiknapp from '../kopiknapp/Kopiknapp';
import StatusGronn from '../../ikoner/Sirkel-gronn.png';
import StatusGul from '../../ikoner/Sirkel-gul.png';
import StatusRod from '../../ikoner/Sirkel-rod.png';
import {logEvent} from "../../api/logger";

export interface TiltakstypelisteProps {
  tiltaksgjennomforingsliste: Array<Tiltaksgjennomforing>;
}

const TiltakstypeTabell = ({ tiltaksgjennomforingsliste }: TiltakstypelisteProps) => {
  const [sort, setSort] = useState<any>();
  const [page, setPage] = useState(1);
  const rowsPerPage = 15;

  const tilgjengelighetsstatus = (status: string) => {
    //TODO endre denne når vi får inn data fra Arena
    if (status === 'Åpent') {
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
              const directionForLogging = sort ? (sortKey === sort.orderBy && sort.direction === 'ascending' ? 'descending' : 'neutral') : 'ascending';
              logEvent('mulighetsrommet.sortering', {sortKey}, {direction: directionForLogging})
           }
        }
      >
        <Table.Header>
          <Table.Row>
            <Table.ColumnHeader
              sortKey="navn"
              sortable
              className="tabell__kolonne__tiltaksnavn"
              data-testid="tabellheader_tiltaksnavn"
            >
              Tiltaksnavn
            </Table.ColumnHeader>
            <Table.ColumnHeader sortKey="id" sortable className="tabell__kolonne__tiltaksnummer">
              Tiltaksnr.
            </Table.ColumnHeader>
            <Table.ColumnHeader sortKey="tiltakskode" sortable className="tabell__kolonne__tiltakstype">
              Tiltakstype
            </Table.ColumnHeader>
            <Table.ColumnHeader sortKey="lokasjon" sortable className="tabell__kolonne__oppstart">
              Lokasjon
            </Table.ColumnHeader>
            <Table.ColumnHeader sortKey="fraDato" sortable className="tabell__kolonne__oppstart">
              Oppstartsdato
            </Table.ColumnHeader>
            <Table.ColumnHeader sortKey="status" sortable className="tabell__kolonne__plasser">
              Status
            </Table.ColumnHeader>
          </Table.Row>
        </Table.Header>
        <Table.Body>
          {tiltaksgjennomforingsliste.length === 0 ? (
            <Table.DataCell colSpan={5}>
              <Alert variant="info" className="tabell__alert">
                Det finnes ingen tiltakstyper med dette søket.
              </Alert>
            </Table.DataCell>
          ) : (
            tiltaksgjennomforingsliste
              .slice()
              .sort((a, b) => {
                if (sort) {
                  const comparator = (a: any, b: any, orderBy: string | number) => {
                    if (b[orderBy] < a[orderBy] || b[orderBy] === undefined) {
                      return -1;
                    }
                    if (b[orderBy] > a[orderBy]) {
                      return 1;
                    }
                    return 0;
                  };
                  return sort.direction === 'ascending'
                    ? comparator(b, a, sort.orderBy)
                    : comparator(a, b, sort.orderBy);
                }
                return 1;
              })
              .slice((page - 1) * rowsPerPage, page * rowsPerPage)
              .map(({ id, tittel, tiltakskode, fraDato, tiltaksnummer }) => (
                <Table.Row key={id}>
                  <Table.DataCell className="tabell__tiltaksnavn">
                    <Lenke to={`/tiltakstyper/${tiltakskode}`} isInline data-testid="tabell_tiltakstyper_tiltaksnummer">
                      {tittel}
                    </Lenke>
                    <div>{'Leverandør'}</div>
                  </Table.DataCell>
                  <Table.DataCell className="tabell__tiltaksnummer" data-testid="tiltaksnummer">
                    {tiltaksnummer}
                    <Kopiknapp kopitekst={tiltaksnummer!} />
                  </Table.DataCell>
                  <Table.DataCell>{tiltakskode}</Table.DataCell>
                  <Table.DataCell>Lokasjon</Table.DataCell>
                  <Table.DataCell>
                    {fraDato ? new Intl.DateTimeFormat().format(new Date(fraDato)) : 'Mangler dato'}
                  </Table.DataCell>
                  <Table.DataCell>{tilgjengelighetsstatus('Åpent')}</Table.DataCell>
                </Table.Row>
              ))
          )}
        </Table.Body>
      </Table>
      <div className="under-tabell">
        <Heading level="1" size="xsmall">
          Viser {tiltaksgjennomforingsliste?.length} av {tiltaksgjennomforingsliste?.length} tiltak
        </Heading>
        <Pagination
          page={page}
          onPageChange={setPage}
          count={Math.ceil(tiltaksgjennomforingsliste.length / rowsPerPage)}
        />
      </div>
    </div>
  );
};

export default TiltakstypeTabell;
