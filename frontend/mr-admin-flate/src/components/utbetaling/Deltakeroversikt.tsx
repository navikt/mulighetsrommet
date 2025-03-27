import { formaterDato } from "@/utils/Utils";
import { Select, Table, TextField } from "@navikt/ds-react";
import { v4 as uuid } from "uuid";

const mockDeltakelser = [
  {
    id: uuid(),
    person: {
      navn: "Per Petterson",
      fodselsdato: "1980-01-01",
      fodselsaar: 1980,
    },
    geografiskTilknytning: "Nav Ullensaker",
    startDato: "2024-06-01",
    forstePeriodeStartDato: "2024-06-01",
    sistePeriodeSluttDato: "2024-06-30",
    sistePeriodeDeltakelsesprosent: 30,
    manedsverk: 0.3,
  },
  {
    id: uuid(),
    person: {
      navn: "Stian Bjærvik",
      fodselsdato: "1986-03-09",
      fodselsaar: 1980,
    },
    geografiskTilknytning: "Nav Ullensaker",
    startDato: "2024-06-01",
    forstePeriodeStartDato: "2024-06-01",
    sistePeriodeSluttDato: "2024-06-30",
    sistePeriodeDeltakelsesprosent: 100,
    manedsverk: 1,
  },
  {
    id: uuid(),
    person: {
      navn: "Donald Duck",
      fodselsdato: "1979-08-12",
      fodselsaar: 1980,
    },
    geografiskTilknytning: "Nav Nannestad Gjerdrum",
    startDato: "2024-06-01",
    forstePeriodeStartDato: "2024-06-01",
    sistePeriodeSluttDato: "2024-06-30",
    sistePeriodeDeltakelsesprosent: 100,
    manedsverk: 1,
  },
];

export function Deltakeroversikt() {
  return (
    <Table>
      <Table.Header>
        <Table.Row>
          <Table.ColumnHeader scope="col">Navn</Table.ColumnHeader>
          <Table.ColumnHeader scope="col">Fødselsdato</Table.ColumnHeader>
          <Table.ColumnHeader scope="col">Geografisk tilknytning</Table.ColumnHeader>
          <Table.ColumnHeader scope="col">Månedsverk</Table.ColumnHeader>
        </Table.Row>
      </Table.Header>
      <Table.Body>
        {mockDeltakelser.map((deltaker) => {
          const { id, person } = deltaker;
          const fodselsdato = formaterDato(person.fodselsdato);
          return (
            <Table.Row key={id}>
              <Table.DataCell className="font-bold">{person.navn}</Table.DataCell>
              <Table.DataCell className="w-52">{fodselsdato}</Table.DataCell>
              <Table.DataCell>{deltaker.geografiskTilknytning}</Table.DataCell>
              <Table.DataCell>{deltaker.manedsverk}</Table.DataCell>
            </Table.Row>
          );
        })}
      </Table.Body>
    </Table>
  );
}
