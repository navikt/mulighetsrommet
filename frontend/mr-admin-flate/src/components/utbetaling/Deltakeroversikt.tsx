import { compareByKey, formaterDato } from "@/utils/Utils";
import { DeltakerForKostnadsfordeling } from "@mr/api-client-v2";
import { SortState, Table, Tag } from "@navikt/ds-react";
import { useState } from "react";

interface Props {
  deltakere: DeltakerForKostnadsfordeling[];
}
interface ScopedSortState extends SortState {
  orderBy: keyof DeltakerForKostnadsfordeling;
}

export function Deltakeroversikt({ deltakere }: Props) {
  const [sort, setSort] = useState<ScopedSortState>();

  const handleSort = (sortKey: ScopedSortState["orderBy"]) => {
    setSort(
      sort && sortKey === sort.orderBy && sort.direction === "descending"
        ? undefined
        : {
            orderBy: sortKey,
            direction:
              sort && sortKey === sort.orderBy && sort.direction === "ascending"
                ? "descending"
                : "ascending",
          },
    );
  };

  const sortedData: DeltakerForKostnadsfordeling[] = [...deltakere]
    .map((deltaker) => ({
      ...deltaker,
      navn: deltaker.navn,
      foedselsdato: deltaker.foedselsdato,
      geografiskEnhet: deltaker.geografiskEnhet,
      region: deltaker.region,
      status: deltaker.status,
      manedsverk: deltaker.manedsverk,
    }))
    .toSorted((a, b) => {
      if (sort) {
        return sort.direction === "ascending"
          ? compareByKey(b, a, sort.orderBy)
          : compareByKey(a, b, sort.orderBy);
      } else {
        return 0;
      }
    });

  return (
    <Table
      sort={sort}
      onSortChange={(sortKey) => handleSort(sortKey as ScopedSortState["orderBy"])}
    >
      <Table.Header>
        <Table.Row>
          <Table.ColumnHeader scope="col">Navn</Table.ColumnHeader>
          <Table.ColumnHeader scope="col">Fødselsdato</Table.ColumnHeader>
          <Table.ColumnHeader scope="col" sortKey="region" sortable>
            Region
          </Table.ColumnHeader>
          <Table.ColumnHeader scope="col" sortKey="geografiskEnhet" sortable>
            Geografisk enhet
          </Table.ColumnHeader>
          <Table.ColumnHeader scope="col">Status</Table.ColumnHeader>
          <Table.ColumnHeader scope="col">Månedsverk</Table.ColumnHeader>
        </Table.Row>
      </Table.Header>
      <Table.Body>
        {sortedData.map((deltaker) => {
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
