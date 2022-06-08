import React, { useEffect, useState } from 'react';
import { Pagination, Table, Alert, Heading } from '@navikt/ds-react';
import './Tabell.less';
import Lenke from '../lenke/Lenke';
import Kopiknapp from '../kopiknapp/Kopiknapp';
import StatusGronn from '../../ikoner/Sirkel-gronn.png';
import StatusGul from '../../ikoner/Sirkel-gul.png';
import StatusRod from '../../ikoner/Sirkel-rod.png';
import { client } from '../../sanityClient';

const TiltakstypeTabell = () => {
  const [sort, setSort] = useState<any>();
  const [page, setPage] = useState(1);
  const rowsPerPage = 15;
  const pagination = (tiltaksgjennomforing: string[]) => {
    return Math.ceil(tiltaksgjennomforing.length / rowsPerPage);
  };

  const [tiltaksgjennomforinger, setTiltaksgjennomforinger] = useState<any>([]); // TODO Se på typing her

  useEffect(() => {
    client
      .query(
        `*[_type == "tiltaksgjennomforing"]{
        _id,
        tiltaksgjennomforingNavn,
        enheter,
        lokasjon,
        oppstart,
        oppstartsdato,
        tiltaksnummer,
        kontaktinfoArrangor->,
        tiltakstype->
        }`
      )
      .then(data => setTiltaksgjennomforinger(data.result));
  }, []);

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
            <Table.ColumnHeader
              sortKey="tiltaksgjennomforingNavn"
              sortable
              className="tabell__kolonne__tiltaksnavn"
              data-testid="tabellheader_tiltaksnavn"
            >
              Tiltaksnavn
            </Table.ColumnHeader>
            <Table.ColumnHeader sortKey="tiltaksnummer" sortable className="tabell__kolonne__tiltaksnummer">
              Tiltaksnr.
            </Table.ColumnHeader>
            {/*TODO fiks sortering*/}
            <Table.ColumnHeader sortKey="tiltakstype" sortable className="tabell__kolonne__tiltakstype">
              Tiltakstype
            </Table.ColumnHeader>
            <Table.ColumnHeader sortKey="lokasjon" sortable className="tabell__kolonne__oppstart">
              Lokasjon
            </Table.ColumnHeader>
            <Table.ColumnHeader sortKey="oppstart" sortable className="tabell__kolonne__oppstart">
              Oppstartsdato
            </Table.ColumnHeader>
            {/*TODO fiks sortering*/}
            <Table.ColumnHeader sortKey="status" sortable className="tabell__kolonne__plasser">
              Status
            </Table.ColumnHeader>
          </Table.Row>
        </Table.Header>
        <Table.Body>
          {tiltaksgjennomforinger.length === 0 ? (
            <Table.Row>
              <Table.DataCell colSpan={5}>
                <Alert variant="info" className="tabell__alert">
                  Det finnes ingen tiltakstyper med dette søket.
                </Alert>
              </Table.DataCell>
            </Table.Row>
          ) : (
            tiltaksgjennomforinger
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
              .map(
                ({
                  _id,
                  tiltaksnummer,
                  tiltaksgjennomforingNavn,
                  oppstart,
                  oppstartsdato,
                  lokasjon,
                  tiltakstype: { tiltakstypeNavn },
                  kontaktinfoArrangor: { selskapsnavn },
                }) => (
                  <Table.Row key={_id}>
                    <Table.DataCell className="tabell__tiltaksnavn">
                      <Lenke to={`/${tiltaksnummer}`} isInline data-testid="tabell_tiltakstyper_tiltaksnummer">
                        {tiltaksgjennomforingNavn}
                      </Lenke>
                      <div>{selskapsnavn}</div>
                    </Table.DataCell>
                    <Table.DataCell className="tabell__tiltaksnummer" data-testid="tiltaksnummer">
                      {tiltaksnummer}
                      <Kopiknapp kopitekst={tiltaksnummer!} />
                    </Table.DataCell>
                    <Table.DataCell>{tiltakstypeNavn}</Table.DataCell>
                    <Table.DataCell>{lokasjon}</Table.DataCell>
                    <Table.DataCell>
                      {oppstart === 'dato' ? new Intl.DateTimeFormat().format(new Date(oppstartsdato)) : 'Løpende'}
                    </Table.DataCell>
                    <Table.DataCell>{tilgjengelighetsstatus('Åpent')}</Table.DataCell>
                  </Table.Row>
                )
              )
          )}
        </Table.Body>
      </Table>
      <div className="under-tabell">
        <Heading level="1" size="xsmall">
          Viser {tiltaksgjennomforinger?.length} av {tiltaksgjennomforinger?.length} tiltak
        </Heading>
        <Pagination
          page={page}
          onPageChange={setPage}
          count={pagination(tiltaksgjennomforinger) === 0 ? 1 : pagination(tiltaksgjennomforinger)}
        />
      </div>
    </div>
  );
};

export default TiltakstypeTabell;
