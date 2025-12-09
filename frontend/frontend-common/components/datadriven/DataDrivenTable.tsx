import { Table, TableProps } from "@navikt/ds-react";
import {
  DataDrivenTableDto,
  DataDrivenTableDtoColumn,
  DataDrivenTableDtoRow,
  DataElement,
} from "./types";
import { DataDrivenTimeline } from "./DataDrivenTimeline";
import { useSortableData } from "../../hooks/useSortableData";
import { compareDataElements } from "./util";
import { getDataElement } from "./DataElement";

interface Props extends TableProps {
  data: DataDrivenTableDto;
  className?: string;
  size?: TableProps["size"];
}

export function DataDrivenTable({ data, className, size, ...rest }: Props) {
  const { sortedData, sort, toggleSort } = useSortableData(
    data.rows,
    undefined,
    (row, key) => row.cells[key],
    compareDataElements,
  );

  const hasExpandableContent = data.rows.some((row) => !!row.content);

  return (
    <Table sort={sort} onSortChange={toggleSort} className={className} size={size} {...rest}>
      <Table.Header>
        <Table.Row>
          {data.columns.map((col) => (
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
        {sortedData.map((row: DataDrivenTableDtoRow, index: number) => {
          if (hasExpandableContent) {
            return (
              <Table.ExpandableRow
                expansionDisabled={!row.content}
                togglePlacement="right"
                key={index}
                content={row.content && <DataDrivenTimeline data={row.content} />}
              >
                <RowContent columns={data.columns} row={row} />
              </Table.ExpandableRow>
            );
          } else {
            return (
              <Table.Row key={index}>
                <RowContent columns={data.columns} row={row} />
              </Table.Row>
            );
          }
        })}
      </Table.Body>
    </Table>
  );
}

interface RowContentProps {
  columns: DataDrivenTableDtoColumn[];
  row: DataDrivenTableDtoRow;
}

function RowContent({ columns, row }: RowContentProps) {
  return (
    <>
      {columns.map((col) => {
        const cells: Record<string, DataElement | null> = row.cells;
        const cell = cells[col.key];
        return (
          <Table.DataCell key={col.key} align={col.align}>
            {cell ? getDataElement(cell) : null}
          </Table.DataCell>
        );
      })}
    </>
  );
}
