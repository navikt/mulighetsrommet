import { Table, TableProps } from "@navikt/ds-react";
import { DataDrivenTableDto, DataDrivenTableDtoRow, DataElement } from "@api-client";
import { compareDataElements, getDataElement } from "../data-element/DataElement";
import { DataDrivenTimeline } from "./DataDrivenTimeline";
import { useSortableData } from "@mr/frontend-common";

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
        {sortedData.map((row: DataDrivenTableDtoRow, index: number) => (
          <Table.ExpandableRow
            expansionDisabled={!row.content}
            togglePlacement="right"
            key={index}
            content={row.content && <DataDrivenTimeline data={row.content} />}
          >
            {data.columns.map((col) => {
              const cells: Record<string, DataElement | null> = row.cells;
              const cell = cells[col.key];
              return (
                <Table.DataCell key={col.key} align={col.align}>
                  {cell ? getDataElement(cell) : null}
                </Table.DataCell>
              );
            })}
          </Table.ExpandableRow>
        ))}
      </Table.Body>
    </Table>
  );
}
