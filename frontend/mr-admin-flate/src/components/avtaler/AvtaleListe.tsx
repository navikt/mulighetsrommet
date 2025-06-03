import { Table } from "@navikt/ds-react";
import { AvtaleDto } from "@mr/api-client-v2";
import { ReactNode } from "react";
import { useAvtaler } from "@/api/avtaler/useAvtaler";
import { AvtaleStatusTag } from "../statuselementer/AvtaleStatusTag";
import { AvtaleFilterType } from "@/pages/avtaler/filter";

interface Props {
  filter: Partial<AvtaleFilterType>;
  action: (avtale: AvtaleDto) => ReactNode;
}

export function AvtaleListe(props: Props) {
  const { data: paginatedAvtaler } = useAvtaler(props.filter);

  return (
    <Table>
      <Table.Header>
        <Table.Row>
          <Table.HeaderCell scope="col">Tittel</Table.HeaderCell>
          <Table.HeaderCell scope="col">Avtalenummer</Table.HeaderCell>
          <Table.HeaderCell scope="col">Status</Table.HeaderCell>
        </Table.Row>
      </Table.Header>
      <Table.Body>
        {paginatedAvtaler.data.map((avtale) => (
          <Table.Row key={avtale.id}>
            <Table.DataCell>{avtale.navn}</Table.DataCell>
            <Table.DataCell>{avtale.avtalenummer}</Table.DataCell>
            <Table.DataCell>
              <AvtaleStatusTag status={avtale.status} />
            </Table.DataCell>
          </Table.Row>
        ))}
      </Table.Body>
    </Table>
  );
}
