import { useTilskuddKompakt } from "@/api/tilskudd/useTilskuddKompakt";
import { useRequiredParams } from "@/hooks/useRequiredParams";
import { useSortableData } from "@mr/frontend-common";
import { Lenke } from "@mr/frontend-common/components/lenke/Lenke";
import { formaterPeriodeSlutt, formaterPeriodeStart } from "@mr/frontend-common/utils/date";
import { Alert, Table } from "@navikt/ds-react";
import { TableColumnHeader } from "@navikt/ds-react/Table";
import { TilskuddKompakt } from "@tiltaksadministrasjon/api-client";

export function TilskuddPage() {
  const { gjennomforingId } = useRequiredParams(["gjennomforingId"]);
  const { data: tilskuddKompakt } = useTilskuddKompakt(gjennomforingId);

  const { sortedData, sort, toggleSort } = useSortableData(
    tilskuddKompakt,
    undefined,
    (item, key) => {
      return key.split(".").reduce((obj: any, k) => obj?.[k], item);
    },
  );
  return (
    <>
      {tilskuddKompakt.length === 0 && (
        <Alert variant="info" className="mt-4">
          Det finnes ingen tilskudd for dette tiltaket
        </Alert>
      )}
      {sortedData.length > 0 && (
        <Table sort={sort} onSortChange={(sortKey) => toggleSort(sortKey as string)}>
          <Table.Header>
            <Table.Row>
              <TableColumnHeader sortKey="periode.start" sortable>
                Periodestart
              </TableColumnHeader>
              <TableColumnHeader sortKey="periode.slutt" sortable>
                Periodeslutt
              </TableColumnHeader>
              <TableColumnHeader sortKey="type" sortable>
                Tilskuddstype
              </TableColumnHeader>
              <TableColumnHeader sortKey="type" sortable>
                Tilskuddsnummer
              </TableColumnHeader>
              <TableColumnHeader sortKey="type" sortable>
                Vedtaksresultat
              </TableColumnHeader>
              <Table.HeaderCell></Table.HeaderCell>
            </Table.Row>
          </Table.Header>
          <Table.Body>
            {sortedData.map((b: TilskuddKompakt) => (
              <Table.Row key={b.id}>
                <Table.DataCell>{b.periode && formaterPeriodeStart(b.periode)}</Table.DataCell>
                <Table.DataCell>{b.periode && formaterPeriodeSlutt(b.periode)}</Table.DataCell>
                <Table.DataCell>{b.type.navn}</Table.DataCell>
                <Table.DataCell>{b.sisteVedtakResultat}</Table.DataCell>
                <Table.DataCell>{b.tilskuddsnummer}</Table.DataCell>
                <Table.DataCell>
                  <Lenke to={b.id}> Detaljer </Lenke>
                </Table.DataCell>
              </Table.Row>
            ))}
          </Table.Body>
        </Table>
      )}
    </>
  );
}
