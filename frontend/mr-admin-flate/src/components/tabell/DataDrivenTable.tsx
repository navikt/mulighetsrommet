import { Table } from "@navikt/ds-react";
import { useState } from "react";
import { DataDrivenColumn, DataDrivenTableDto } from "@mr/api-client-v2";

interface Props {
  data: DataDrivenTableDto;
  className?: string;
}

export function DataDrivenTable({ data, className }: Props) {
  const [sort, setSort] = useState<{ orderBy: string; direction: "ascending" | "descending" }>();

  const sortedRows = [...data.rows].sort((a, b) => {
    if (!sort) return 0;
    const aVal = a[sort.orderBy];
    const bVal = b[sort.orderBy];
    return sort.direction === "ascending"
      ? String(aVal).localeCompare(String(bVal))
      : String(bVal).localeCompare(String(aVal));
  });

  return (
    <Table className={className} size="small">
      <Table.Header>
        <Table.Row>
          {data.columns.map((col: DataDrivenColumn) => (
            <Table.ColumnHeader
              key={col.key}
              sortKey={col.sortable ? col.key : undefined}
              sortable={col.sortable}
              onClick={() =>
                col.sortable &&
                setSort((prev) =>
                  prev?.orderBy === col.key && prev.direction === "ascending"
                    ? { orderBy: col.key, direction: "descending" }
                    : { orderBy: col.key, direction: "ascending" },
                )
              }
            >
              {col.label}
            </Table.ColumnHeader>
          ))}
        </Table.Row>
      </Table.Header>
      <Table.Body>
        {sortedRows.map((row) => (
          <Table.Row key={row.id}>
            {data.columns.map((col) => (
              <Table.DataCell key={col.key} align={col.align}>
                {row[col.key] ?? "-"}
              </Table.DataCell>
            ))}
          </Table.Row>
        ))}
      </Table.Body>
    </Table>
  );
}
