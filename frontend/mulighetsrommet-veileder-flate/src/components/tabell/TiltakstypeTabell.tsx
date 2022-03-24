import React from 'react';
import { Alert, Heading, Table } from '@navikt/ds-react';
import Lenke from '../lenke/Lenke';
import './Tabell.less';
import { Tiltakstype } from 'mulighetsrommet-api';

export interface TiltakstypelisteProps {
  tiltakstypeliste: Array<Tiltakstype>;
}

const TiltakstypeTabell = ({ tiltakstypeliste }: TiltakstypelisteProps) => {
  return (
    <Table zebraStripes size="small" data-testid="tabell_tiltakstyper">
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
                  <Lenke
                    to={`/tiltakstyper/${tiltakstype.tiltakskode}`}
                    isInline
                    data-testid="tabell_tiltakstyper_tiltaksnummer"
                  >
                    {tiltakstype.id}
                  </Lenke>
                </Table.HeaderCell>
                <Table.DataCell>
                  <Heading size="xsmall">{tiltakstype.navn}</Heading>
                  Arrangørnavn
                </Table.DataCell>
                <Table.DataCell>{tiltakstype.navn}</Table.DataCell>
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
