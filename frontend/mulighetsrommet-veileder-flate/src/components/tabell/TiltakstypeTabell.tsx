import React, { useState } from 'react';
import { Pagination, Table, Alert, Label } from '@navikt/ds-react';
import './Tabell.less';
import '../../App.less';
import { Tiltakstype } from '../../../../mulighetsrommet-api-client';

export interface TiltakstypelisteProps {
  tiltakstypeliste: Array<Tiltakstype>;
}

const TiltakstypeTabell = ({ tiltakstypeliste }: TiltakstypelisteProps) => {
  const [sort, setSort] = useState<any>();
  const [page, setPage] = useState(1);
  const rowsPerPage = 10;

  return (
    <div className="w-full flex flex-col gap-4">
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
            <Table.ColumnHeader sortKey="id" sortable className="tabell__kolonne__tiltaksnummer">
              Tiltaksnr.
            </Table.ColumnHeader>
            <Table.ColumnHeader sortKey="navn" sortable className="tabell__kolonne__tiltaksnavn">
              Tiltaksnavn
            </Table.ColumnHeader>
            <Table.ColumnHeader sortKey="tiltakskode" sortable className="tabell__kolonne__tiltakstype">
              Tiltakstype
            </Table.ColumnHeader>
            <Table.ColumnHeader sortKey="fraDato" sortable className="tabell__kolonne__oppstart">
              Oppstartsdato
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
              .map(({ id, tiltakskode, fraDato, navn }) => (
                <Table.Row
                  key={id}
                  onClick={() => (location.href = `/tiltakstyper/${tiltakskode}`)}
                  className="row-btn"
                  data-testid="tabell_tiltakstyper_rad"
                >
                  <Table.DataCell>{id}</Table.DataCell>
                  <Table.DataCell className="tabell__tiltaksnavn">
                    <Label>{navn}</Label>
                    {'Leverandør'}
                  </Table.DataCell>
                  <Table.DataCell>{tiltakskode}</Table.DataCell>
                  <Table.DataCell>{new Date(fraDato!).toLocaleDateString()}</Table.DataCell>
                  <Table.DataCell>Plasser</Table.DataCell>
                </Table.Row>
              ))
          )}
        </Table.Body>
      </Table>
      <Pagination page={page} onPageChange={setPage} count={Math.ceil(tiltakstypeliste.length / rowsPerPage)} />
    </div>
  );
};

export default TiltakstypeTabell;
