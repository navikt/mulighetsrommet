import { Table } from "@navikt/ds-react";
import { DataDrivenColumn, DataDrivenTableDto } from "@mr/api-client-v2";
import { formaterNOK } from "@mr/frontend-common/utils/utils";
import { useSortableData } from "@mr/frontend-common";
import { formaterDato } from "@mr/frontend-common/utils/date";

interface Props {
  data: DataDrivenTableDto;
  className?: string;
}

export function DataDrivenTable({ data, className }: Props) {
  const { sort, toggleSort, sortedData } = useSortableData(data.rows);

  function formatRow(format: "DATE" | "NOK" | null, row?: string): string {
    if (!row) return "-";
    switch (format) {
      case "DATE":
        return formaterDato(row) ?? "-";
      case "NOK":
        return formaterNOK(Number(row));
      case null:
        return row;
    }
  }

  return (
    <Table
      sort={sort}
      onSortChange={(sortKey) => toggleSort(sortKey as string)}
      className={className}
      size="small"
    >
      <Table.Header>
        <Table.Row>
          {data.columns.map((col: DataDrivenColumn) => (
            <Table.ColumnHeader
              key={col.key}
              sortable={col.sortable}
              sortKey={col.key}
              align={col.align}
            >
              {col.label}
            </Table.ColumnHeader>
          ))}
        </Table.Row>
      </Table.Header>
      <Table.Body>
        {sortedData.map((row, index) => (
          <Table.Row key={index}>
            {data.columns.map((col) => (
              <Table.DataCell key={col.key} align={col.align}>
                {formatRow(col.format, row[col.key])}
              </Table.DataCell>
            ))}
          </Table.Row>
        ))}
      </Table.Body>
    </Table>
  );
}
