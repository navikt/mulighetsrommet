import React from 'react';
import { Table } from '@navikt/ds-react';
import Lenke from '../lenke/Lenke';
import { Tiltaksvariant } from '../../api';

export interface TiltaksvariantlisteProps {
  tiltaksvariantliste: Array<Tiltaksvariant>;
}

const TiltaksvariantTabell = ({ tiltaksvariantliste }: TiltaksvariantlisteProps) => {
  return (
    <Table zebraStripes data-testid="tabell_oversikt-tiltaksvarianter">
      <Table.Header>
        <Table.Row>
          <Table.HeaderCell scope="col">Tittel</Table.HeaderCell>
          <Table.HeaderCell scope="col">Ingress</Table.HeaderCell>
        </Table.Row>
      </Table.Header>
      <Table.Body>
        {tiltaksvariantliste.map((tiltaksvariant: Tiltaksvariant) => (
          <Table.Row key={tiltaksvariant.id} className="tabell__row">
            <Table.HeaderCell scope="col">
              <Lenke to={`/tiltaksvarianter/${tiltaksvariant.id}`} isInline>
                {tiltaksvariant.tittel}
              </Lenke>
            </Table.HeaderCell>
            <Table.DataCell className="tabell__row__ingress">{tiltaksvariant.ingress}</Table.DataCell>
          </Table.Row>
        ))}
      </Table.Body>
    </Table>
  );
};

export default TiltaksvariantTabell;
