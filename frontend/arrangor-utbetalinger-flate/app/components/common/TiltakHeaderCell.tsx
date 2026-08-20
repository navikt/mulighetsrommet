import { BodyShort, Table } from "@navikt/ds-react";

interface Props {
  tiltakstype: string;
  navn: string;
  lopenummer: string;
}

export function TiltakHeaderCell({ tiltakstype, navn, lopenummer }: Props) {
  return (
    <Table.HeaderCell scope="row">
      <strong>{tiltakstype}</strong>
      <BodyShort>
        {navn} ({lopenummer})
      </BodyShort>
    </Table.HeaderCell>
  );
}
