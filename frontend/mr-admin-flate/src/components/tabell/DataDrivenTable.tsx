import { Table } from "@navikt/ds-react";
import { DataDrivenColumn, DataDrivenTableDto } from "@mr/api-client-v2";
import { formaterDato } from "@/utils/Utils";
import { formaterNOK } from "@mr/frontend-common/utils/utils";
import { useSortableData } from "@mr/frontend-common";

interface Props {
  data: DataDrivenTableDto;
  className?: string;
}

export function DataDrivenTable({ data, className }: Props) {
  const { sort, toggleSort, sortedData } = useSortableData(data.rows);

  function formatRow(row?: string, format?: "DATE" | "NOK"): string {
    if (!row) return "-";
    switch (format) {
      case "DATE":
        return formaterDato(row);
      case "NOK":
        return formaterNOK(Number(row));
      default:
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
            <Table.ColumnHeader key={col.key} sortable={col.sortable} sortKey={col.key}>
              {col.label}
            </Table.ColumnHeader>
          ))}
        </Table.Row>
      </Table.Header>
      <Table.Body>
        {sortedData.map((row) => (
          <Table.Row key={row.id}>
            {data.columns.map((col) => (
              <Table.DataCell key={col.key} align={col.align}>
                {formatRow(row[col.key], col.format)}
              </Table.DataCell>
            ))}
          </Table.Row>
        ))}
      </Table.Body>
    </Table>
  );
}
