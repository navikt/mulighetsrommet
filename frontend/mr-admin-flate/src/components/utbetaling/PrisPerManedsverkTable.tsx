import { formaterDato } from "@/utils/Utils";
import { DeltakerForKostnadsfordeling } from "@mr/api-client-v2";
import { useSortableData } from "@mr/frontend-common";
import { BodyShort, CopyButton, HStack, Table, VStack } from "@navikt/ds-react";

interface Props {
  deltakere: DeltakerForKostnadsfordeling[];
  sats: number;
  maxHeight: string;
}

export function PrisPerManedsverkTable({ deltakere, sats, maxHeight: height }: Props) {
  const { sortedData, sort, toggleSort } = useSortableData(deltakere);

  function totalManedsverk() {
    return sortedData.reduce((sum, deltaker) => sum + deltaker.manedsverk, 0);
  }

  function totalBelop() {
    return Math.round(roundNdecimals(totalManedsverk(), 5) * sats);
  }

  function roundNdecimals(num: number, N: number) {
    return Number(num.toFixed(N));
  }

  return (
    <VStack>
      <div className={`max-h-[${height}] overflow-y-scroll`}>
        <Table
          size="small"
          sort={sort}
          onSortChange={(sortKey) => toggleSort(sortKey as keyof DeltakerForKostnadsfordeling)}
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
                  <Table.DataCell>{roundNdecimals(manedsverk, 2)}</Table.DataCell>
                  <Table.DataCell align="right">
                    {roundNdecimals(manedsverk * sats, 2)}
                  </Table.DataCell>
                </Table.Row>
              );
            })}
          </Table.Body>
          <Table.Row className="sticky bottom-0 bg-white z-10">
            <Table.DataCell colSpan={4} />
            <Table.DataCell>{roundNdecimals(totalManedsverk(), 2)}</Table.DataCell>
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
        <BodyShort>{`Beregning: ${roundNdecimals(totalManedsverk(), 5)} × ${sats} = ${totalBelop()}`}</BodyShort>
      </HStack>
    </VStack>
  );
}
