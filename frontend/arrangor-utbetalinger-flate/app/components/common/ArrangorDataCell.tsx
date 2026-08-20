import { Table } from "@navikt/ds-react";
import { ArrangorflateArrangorDto } from "@arrangor-utbetalinger/api-client";

interface Props {
  arrangor: ArrangorflateArrangorDto;
}

export function ArrangorDataCell({ arrangor }: Props) {
  return (
    <Table.DataCell>
      {arrangor.navn} ({arrangor.organisasjonsnummer})
    </Table.DataCell>
  );
}
