import React from 'react';
import { useLocation } from 'react-router-dom';
import Lenke from '../lenke/Lenke';
import { Table } from '@navikt/ds-react';
import { Tiltaksgjennomforing } from '../../api';
import '../../views/ViewTiltakstype-tiltaksgjennomforing-detaljer.less';

interface TiltaksgjennomforingTabellProps {
  tiltaksgjennomforinger?: Tiltaksgjennomforing[];
}

function TiltaksgjennomforingsTabell(props: TiltaksgjennomforingTabellProps) {
  const location = useLocation();
  const { tiltaksgjennomforinger } = props;

  return (
    <Table
      zebraStripes
      size="small"
      className="tabell__tiltaksgjennomforinger"
      data-testid="tabell_tiltaksgjennomforinger"
    >
      <Table.Header>
        <Table.Row>
          <Table.HeaderCell className="tabell__tiltaksgjennomforinger__tittel">Tittel</Table.HeaderCell>
          <Table.HeaderCell className="tabell__tiltaksgjennomforinger__tiltaksnummer">Tiltaksnummer</Table.HeaderCell>
          <Table.HeaderCell className="tabell__tiltaksgjennomforinger__fra-dato">Fra dato</Table.HeaderCell>
          <Table.HeaderCell className="tabell__tiltaksgjennomforinger__til-dato">Til dato</Table.HeaderCell>
        </Table.Row>
      </Table.Header>
      <Table.Body>
        {tiltaksgjennomforinger?.map(tiltaksgjennomforing => (
          <Table.Row key={tiltaksgjennomforing.id} className="tabell__row">
            <Table.HeaderCell>
              <Lenke to={`${location.pathname}/tiltaksgjennomforinger/${tiltaksgjennomforing.id}`}>
                {tiltaksgjennomforing.tittel}
              </Lenke>
            </Table.HeaderCell>
            <Table.DataCell>{tiltaksgjennomforing.tiltaksnummer}</Table.DataCell>
            <Table.DataCell>{tiltaksgjennomforing.fraDato}</Table.DataCell>
            <Table.DataCell>{tiltaksgjennomforing.tilDato}</Table.DataCell>
          </Table.Row>
        ))}
      </Table.Body>
    </Table>
  );
}

export default TiltaksgjennomforingsTabell;
