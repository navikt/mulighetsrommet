import { Table } from "@navikt/ds-react";

interface Props {
  navn: string;
  organisasjonsnummer: string;
}

export function ArrangorDataCell({ navn, organisasjonsnummer }: Props) {
  return (
    <Table.DataCell>
      {navn} ({organisasjonsnummer})
    </Table.DataCell>
  );
}
