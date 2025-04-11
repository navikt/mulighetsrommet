import { formaterDato } from "@/utils/Utils";
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
          <Table.ColumnHeader scope="col">Navn</Table.ColumnHeader>
          <Table.ColumnHeader scope="col">Fødselsdato</Table.ColumnHeader>
          <Table.ColumnHeader scope="col">Region</Table.ColumnHeader>
          <Table.ColumnHeader scope="col">Geografisk enhet</Table.ColumnHeader>
          <Table.ColumnHeader scope="col">Status</Table.ColumnHeader>
          <Table.ColumnHeader scope="col">Månedsverk</Table.ColumnHeader>
        </Table.Row>
      </Table.Header>
      <Table.Body>
        {deltakere.map((deltaker) => {
          const { id, navn, foedselsdato, region, geografiskEnhet, status, manedsverk } = deltaker;
          const capitalizedStatus = status.charAt(0).toUpperCase() + status.slice(1).toLowerCase();

          return (
            <Table.Row key={id}>
              <Table.DataCell className="font-bold">{navn ?? "-"}</Table.DataCell>
              <Table.DataCell>{formaterDato(foedselsdato) ?? "-"}</Table.DataCell>
              <Table.DataCell>{region ?? "-"}</Table.DataCell>
              <Table.DataCell>{geografiskEnhet ?? "-"}</Table.DataCell>
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
