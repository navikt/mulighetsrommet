import React from 'react';
import { Table } from '@navikt/ds-react';
import Lenke from '../lenke/Lenke';
import { Tiltakstype } from '../../api';

export interface TiltakstypelisteProps {
  tiltakstypeliste: Array<Tiltakstype>;
}

const TiltakstypeTabell = ({ tiltakstypeliste }: TiltakstypelisteProps) => {
  return (
    <Table zebraStripes data-testid="tabell_oversikt-tiltakstyper">
      <Table.Header>
        <Table.Row>
          <Table.HeaderCell scope="col">Tittel</Table.HeaderCell>
          <Table.HeaderCell scope="col">Ingress</Table.HeaderCell>
        </Table.Row>
      </Table.Header>
      <Table.Body>
        {tiltakstypeliste.map((tiltakstype: Tiltakstype) => (
          <Table.Row key={tiltakstype.id} className="tabell__row">
            <Table.HeaderCell scope="col">
              <Lenke to={`/tiltakstyper/${tiltakstype.tiltakskode}`} isInline>
                {tiltakstype.navn}
              </Lenke>
            </Table.HeaderCell>
            {/* Ingress er fjernet fra tiltakstype. Dette må vi håndtere i Sanity */}
            <Table.DataCell className="tabell__row__ingress">INGRESS FRA SANITY</Table.DataCell>
          </Table.Row>
        ))}
      </Table.Body>
    </Table>
  );
};

export default TiltakstypeTabell;
