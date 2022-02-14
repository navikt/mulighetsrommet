import React from 'react';
import { Alert, Table } from '@navikt/ds-react';
import Lenke from '../lenke/Lenke';
import { Tiltaksvariant } from '../../api';
import './Tabell.less';

export interface TiltaksvariantlisteProps {
  tiltaksvariantliste: Array<Tiltaksvariant>;
}

const TiltaksvariantTabell = ({ tiltaksvariantliste }: TiltaksvariantlisteProps) => {
  return (
    <Table zebraStripes size="small" data-testid="tabell__oversikt-tiltaksvarianter">
      <Table.Header>
        <Table.Row>
          <Table.HeaderCell className="tabell-tiltaksvarianter__tittel">Tittel</Table.HeaderCell>
          <Table.HeaderCell>Ingress</Table.HeaderCell>
        </Table.Row>
      </Table.Header>
      <Table.Body>
        {tiltaksvariantliste.length === 0 ? (
          <Table.Row>
            <Table.DataCell>
              <Alert variant="info" className="tabell__alert">
                Det finnes ingen tiltaksvarianter med dette søket.
              </Alert>
            </Table.DataCell>
          </Table.Row>
        ) : (
          <>
            {tiltaksvariantliste.map((tiltaksvariant: Tiltaksvariant) => (
              <Table.Row key={tiltaksvariant.id}>
                <Table.HeaderCell>
                  <Lenke to={`/tiltaksvarianter/${tiltaksvariant.id}`} isInline>
                    {tiltaksvariant.tittel}
                  </Lenke>
                </Table.HeaderCell>
                <Table.DataCell>{tiltaksvariant.ingress}</Table.DataCell>
              </Table.Row>
            ))}
          </>
        )}
      </Table.Body>
    </Table>
  );
};

export default TiltaksvariantTabell;
