import { DeltakerForKostnadsfordeling } from "@mr/api-client-v2";
import { Table, Tag } from "@navikt/ds-react";

interface Props {
  deltakere: DeltakerForKostnadsfordeling[];
}

export function Deltakeroversikt({ deltakere }: Props) {
  return (
    <Table>
      <Table.Header>
        <Table.Row>
          <Table.ColumnHeader scope="col">Fødselsnummer</Table.ColumnHeader>
          <Table.ColumnHeader scope="col">Status</Table.ColumnHeader>
          <Table.ColumnHeader scope="col">Månedsverk</Table.ColumnHeader>
        </Table.Row>
      </Table.Header>
      <Table.Body>
        {deltakere.map((deltaker) => {
          const { id, fnr, status, manedsverk } = deltaker;
          const capitalizedStatus = status.charAt(0).toUpperCase() + status.slice(1).toLowerCase();

          return (
            <Table.Row key={id}>
              <Table.DataCell className="font-bold">{fnr ?? "Ukjent fnr"}</Table.DataCell>
              <Table.DataCell>
                <Tag children={capitalizedStatus} variant={"info"}></Tag>
              </Table.DataCell>
              <Table.DataCell>{manedsverk}</Table.DataCell>
            </Table.Row>
          );
        })}
      </Table.Body>
    </Table>
  );
}
