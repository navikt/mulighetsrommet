import React, { useState } from 'react';
import { Pagination, Table, BodyShort, Alert } from '@navikt/ds-react';
import './Tabell.less';
import { Tiltakstype } from 'mulighetsrommet-api';
import Lenke from '../lenke/Lenke';

export interface TiltakstypelisteProps {
  tiltakstypeliste: Array<Tiltakstype>;
}

const TiltakstypeTabell = ({ tiltakstypeliste }: TiltakstypelisteProps) => {
  const [sort, setSort] = useState<any>();
  const [page, setPage] = useState(1);
  const rowsPerPage = 10;

  return (
    <>
      <Table
        zebraStripes
        size="small"
        sort={sort}
        data-testid="tabell_tiltakstyper"
        className="tabell"
        onSortChange={sortKey =>
          setSort(
            sort && sortKey === sort.orderBy && sort.direction === 'descending'
              ? undefined
              : {
                  orderBy: sortKey,
                  direction:
                    sort && sortKey === sort.orderBy && sort.direction === 'ascending' ? 'descending' : 'ascending',
                }
          )
        }
      >
        <Table.Header>
          <Table.Row>
            <Table.ColumnHeader sortKey="tiltaksnummer" sortable className="tabell__kolonne__tiltaksnummer">
              Tiltaksnr.
            </Table.ColumnHeader>
            <Table.ColumnHeader sortKey="tiltaksnavn" sortable className="tabell__kolonne__tiltaksnavn">
              Tiltaksnavn
            </Table.ColumnHeader>
            <Table.ColumnHeader sortKey="tiltakstype" sortable className="tabell__kolonne__tiltakstype">
              Tiltakstype
            </Table.ColumnHeader>
            <Table.ColumnHeader sortKey="oppstart" sortable className="tabell__kolonne__oppstart">
              Oppstart
            </Table.ColumnHeader>
            <Table.ColumnHeader className="tabell__kolonne__plasser">Plasser/Ventetid</Table.ColumnHeader>
          </Table.Row>
        </Table.Header>
        <Table.Body>
          {tiltakstypeliste.length === 0 ? (
            <Table.DataCell colSpan={5}>
              <Alert variant="info" className="tabell__alert">
                Det finnes ingen tiltakstyper med dette søket.
              </Alert>
            </Table.DataCell>
          ) : (
            tiltakstypeliste
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
              .map(({ id, tiltakskode, navn, fraDato }) => (
                <Table.Row key={id}>
                  <Table.HeaderCell>{id}</Table.HeaderCell>
                  <Table.DataCell>
                    <Lenke
                      to={`/tiltakstyper/${tiltakskode}`}
                      className="tabell__tiltaksnavn"
                      data-testid="tabell_tiltakstyper_tiltaksnummer"
                    >
                      {navn}
                    </Lenke>
                    <BodyShort>Leverandør</BodyShort>
                  </Table.DataCell>
                  <Table.DataCell>{tiltakskode}</Table.DataCell>
                  <Table.DataCell>{fraDato}</Table.DataCell>
                  <Table.DataCell>Plasser</Table.DataCell>
                </Table.Row>
              ))
          )}
        </Table.Body>
      </Table>
      <Pagination page={page} onPageChange={setPage} count={Math.ceil(tiltakstypeliste.length / rowsPerPage)} />
    </>
  );
};

export default TiltakstypeTabell;
