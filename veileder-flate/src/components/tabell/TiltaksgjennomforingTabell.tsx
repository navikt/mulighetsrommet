import React from 'react';
import { useLocation } from 'react-router-dom';
import Lenke from '../lenke/Lenke';
import { Table } from '@navikt/ds-react';
import { Tiltaksgjennomforing } from 'mulighetsrommet-api-client';

interface TiltaksgjennomforingTabellProps {
  tiltaksgjennomforinger?: Tiltaksgjennomforing[];
}

function TiltaksgjennomforingsTabell(props: TiltaksgjennomforingTabellProps) {
  const location = useLocation();
  const { tiltaksgjennomforinger } = props;

  return (
    <Table zebraStripes className="tabell__tiltaksgjennomforing" data-testid="tabell_tiltaksgjennomforinger">
      <Table.Header>
        <Table.Row>
          <Table.HeaderCell scope="col">Tittel</Table.HeaderCell>
          <Table.HeaderCell scope="col">Tiltaksnummer</Table.HeaderCell>
          <Table.HeaderCell scope="col">Til dato</Table.HeaderCell>
          <Table.HeaderCell scope="col">Fra dato</Table.HeaderCell>
        </Table.Row>
      </Table.Header>
      <Table.Body>
        {tiltaksgjennomforinger?.map(tiltaksgjennomforing => (
          <Table.Row key={tiltaksgjennomforing.id} className="tabell__row">
            <Table.HeaderCell scope="col">
              <Lenke to={`${location.pathname}/tiltaksgjennomforinger/${tiltaksgjennomforing.id}`}>
                {tiltaksgjennomforing.tittel}
              </Lenke>
            </Table.HeaderCell>
            <Table.DataCell>{tiltaksgjennomforing.tiltaksnummer}</Table.DataCell>
            <Table.DataCell>{tiltaksgjennomforing.tilDato}</Table.DataCell>
            <Table.DataCell>{tiltaksgjennomforing.fraDato}</Table.DataCell>
          </Table.Row>
        ))}
      </Table.Body>
    </Table>
  );
}

export default TiltaksgjennomforingsTabell;
