import { useSortableData } from "@mr/frontend-common";
import { Table, TableProps } from "@navikt/ds-react";
import { DataDrivenTableDto, DataElement } from "@api-client";
import { compareDataElements, getDataElement } from "../data-element/DataElement";

interface Props extends TableProps {
  data: DataDrivenTableDto;
  className?: string;
  size?: TableProps["size"];
}

export function DataDrivenTable({ data, className, size, ...rest }: Props) {
  const { sort, toggleSort, sortedData } = useSortableData(
    data.rows,
    undefined,
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
        {sortedData.map((row: Record<string, DataElement | null>, index) => (
          <Table.Row key={index}>
            {data.columns.map((col) => {
              const cell = row[col.key];
              return (
                <Table.DataCell key={col.key} align={col.align}>
                  {cell ? getDataElement(cell) : null}
                </Table.DataCell>
              );
            })}
          </Table.Row>
        ))}
      </Table.Body>
    </Table>
  );
}
