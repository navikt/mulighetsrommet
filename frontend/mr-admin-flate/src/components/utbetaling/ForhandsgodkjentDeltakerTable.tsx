import { compareByKey, formaterDato } from "@/utils/Utils";
import { DeltakerForKostnadsfordeling } from "@mr/api-client-v2";
import { CopyButton, HStack, SortState, Table, VStack } from "@navikt/ds-react";
import { useState } from "react";

interface Props {
  deltakere: DeltakerForKostnadsfordeling[];
  sats: number;
}
interface ScopedSortState extends SortState {
  orderBy: keyof DeltakerForKostnadsfordeling;
}

export function ForhandsgodkjentDeltakerTable({ deltakere, sats }: Props) {
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

  function totalManedsverk() {
    return (
      Math.round(sortedData.reduce((sum, deltaker) => sum + deltaker.manedsverk, 0) * 100) / 100
    );
  }

  function totalBelop() {
    return Math.round(totalManedsverk() * sats * 100) / 100;
  }

  return (
    <VStack maxHeight="400px">
      <div style={{ overflowY: "auto", flexGrow: 1 }}>
        <Table
          size="small"
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
              <Table.ColumnHeader scope="col" sortKey="manedsverk" sortable>
                Månedsverk
              </Table.ColumnHeader>
              <Table.ColumnHeader scope="col" sortKey="manedsverk" sortable>
                Beløp
              </Table.ColumnHeader>
            </Table.Row>
          </Table.Header>
          <Table.Body>
            {sortedData.map((deltaker: DeltakerForKostnadsfordeling) => {
              const { id, navn, foedselsdato, region, geografiskEnhet, manedsverk } = deltaker;

              return (
                <Table.Row key={id}>
                  <Table.DataCell className="font-bold">{navn ?? "-"}</Table.DataCell>
                  <Table.DataCell>{formaterDato(foedselsdato) ?? "-"}</Table.DataCell>
                  <Table.DataCell>{region?.navn ?? "-"}</Table.DataCell>
                  <Table.DataCell>{geografiskEnhet?.navn ?? "-"}</Table.DataCell>
                  <Table.DataCell align="right">{manedsverk}</Table.DataCell>
                  <Table.DataCell align="right">
                    {Math.round(manedsverk * sats * 100) / 100}
                  </Table.DataCell>
                </Table.Row>
              );
            })}
          </Table.Body>
        </Table>
      </div>
      <HStack align="start" justify="end">
        <CopyButton
          variant="action"
          copyText={totalBelop().toString()}
          size="small"
          text={`${totalManedsverk()} × ${sats} = ${totalBelop().toString()}`}
        />
      </HStack>
    </VStack>
  );
}
