import React from 'react';
import { Alert, Table } from '@navikt/ds-react';
import Lenke from '../lenke/Lenke';
import './Tabell.less';
import { Tiltakstype } from '../../api';

export interface TiltakstypelisteProps {
  tiltakstypeliste: Array<Tiltakstype>;
}

const TiltakstypeTabell = ({ tiltakstypeliste }: TiltakstypelisteProps) => {
  return (
    <Table zebraStripes size="small" data-testid="tabell__oversikt-tiltakstyper">
      <Table.Header>
        <Table.Row>
          <Table.HeaderCell className="tabell-tiltakstyper__tiltaksnummer">Tiltaksnummer</Table.HeaderCell>
          <Table.HeaderCell>Tiltaksnavn</Table.HeaderCell>
          <Table.HeaderCell>Tiltakstype</Table.HeaderCell>
          <Table.HeaderCell>Innsatsgruppe</Table.HeaderCell>
        </Table.Row>
      </Table.Header>
      <Table.Body>
        {tiltakstypeliste.length === 0 ? (
          <Table.DataCell colSpan={2}>
            <Alert variant="info" className="tabell__alert">
              Det finnes ingen tiltakstyper med dette søket.
            </Alert>
          </Table.DataCell>
        ) : (
          <>
            {tiltakstypeliste.map((tiltakstype: Tiltakstype) => (
              <Table.Row key={tiltakstype.id}>
                <Table.HeaderCell>
                  <Lenke to={`/tiltakstyper/${tiltakstype.id}`} isInline>
                    {tiltakstype.id}
                  </Lenke>
                </Table.HeaderCell>
                <Table.DataCell>{tiltakstype.tittel}</Table.DataCell>
                <Table.DataCell>{'Tiltakstype'}</Table.DataCell>
                <Table.DataCell>{tiltakstype.innsatsgruppe}</Table.DataCell>
              </Table.Row>
            ))}
          </>
        )}
      </Table.Body>
    </Table>
  );
};

export default TiltakstypeTabell;
