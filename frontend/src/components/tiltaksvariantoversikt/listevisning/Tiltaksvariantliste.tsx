import React from 'react';
import { Tiltaksvariant } from '../../../core/domain/Tiltaksvariant';
import '@navikt/ds-css';
import { Table } from '@navikt/ds-react';
import Lenke from '../../link/Lenke';

export interface TiltaksvariantlisteProps {
  tiltaksvariantliste?: Array<Tiltaksvariant>;
}

const Tiltaksvariantliste = ({ tiltaksvariantliste }: TiltaksvariantlisteProps) => {
  return (
    <Table zebraStripes data-testid="tabell_oversikt-tiltaksvarianter">
      <Table.Header>
        <Table.Row>
          <Table.HeaderCell scope="col">Tittel</Table.HeaderCell>
          <Table.HeaderCell scope="col">Ingress</Table.HeaderCell>
        </Table.Row>
      </Table.Header>
      <Table.Body>
        {tiltaksvariantliste?.map((tiltaksvariant: Tiltaksvariant) => (
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

export default Tiltaksvariantliste;
