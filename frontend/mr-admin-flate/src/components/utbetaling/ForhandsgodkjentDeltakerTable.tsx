import { compareByKey, formaterDato } from "@/utils/Utils";
import { DeltakerForKostnadsfordeling } from "@mr/api-client-v2";
import { BodyShort, CopyButton, HStack, SortState, Table, VStack } from "@navikt/ds-react";
import { useState } from "react";

interface Props {
  deltakere: DeltakerForKostnadsfordeling[];
  sats: number;
  maxHeight: string;
}
interface ScopedSortState extends SortState {
  orderBy: keyof DeltakerForKostnadsfordeling;
}

export function ForhandsgodkjentDeltakerTable({ deltakere, sats, maxHeight: height }: Props) {
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
    return sortedData.reduce((sum, deltaker) => sum + deltaker.manedsverk, 0);
  }

  function totalBelop() {
    return Math.round(totalManedsverk() * sats);
  }

  function round2decimals(n: number): number {
    return Math.round(n * 100) / 100;
  }

  return (
    <VStack>
      <div className={`max-h-[${height}] overflow-y-scroll`}>
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
                  <Table.DataCell>{round2decimals(manedsverk)}</Table.DataCell>
                  <Table.DataCell align="right">{round2decimals(manedsverk * sats)}</Table.DataCell>
                </Table.Row>
              );
            })}
          </Table.Body>
          <Table.Row className="sticky bottom-0 bg-white z-10">
            <Table.DataCell colSpan={4} />
            <Table.DataCell>{round2decimals(totalManedsverk())}</Table.DataCell>
            <Table.DataCell>
              <HStack justify="end">
                <CopyButton
                  variant="action"
                  copyText={totalBelop().toString()}
                  size="small"
                  text={totalBelop().toString()}
                />
              </HStack>
            </Table.DataCell>
          </Table.Row>
        </Table>
      </div>
      <HStack className="pt-4" align="start" justify="end">
        <BodyShort>
          {`Beregning: ${round2decimals(totalManedsverk())} × ${sats} = ${totalBelop()}`}
        </BodyShort>
      </HStack>
    </VStack>
  );
}
