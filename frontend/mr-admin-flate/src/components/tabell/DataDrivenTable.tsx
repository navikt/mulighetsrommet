import {
  DataDrivenTableDto,
  DataDrivenTableDtoRow,
  DataElement,
} from "@tiltaksadministrasjon/api-client";
import { useSortableData } from "@mr/frontend-common";
import { Table, TableProps } from "@navikt/ds-react";
import { compareDataElements, getDataElement } from "@/components/data-element/DataElement";

interface Props {
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
          <Table.ExpandableRow expansionDisabled togglePlacement="right" key={index} content={null}>
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
