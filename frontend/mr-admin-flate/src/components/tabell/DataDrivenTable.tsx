import { Table } from "@navikt/ds-react";
import { DataDrivenTableDto, DataElement, DataElementTextFormat } from "@mr/api-client-v2";
import { compare, formaterNOK, formaterTall } from "@mr/frontend-common/utils/utils";
import { StatusTag, useSortableData } from "@mr/frontend-common";
import { formaterDato } from "@mr/frontend-common/utils/date";
import { Lenke } from "@mr/frontend-common/components/lenke/Lenke";
import { ReactNode } from "react";

interface Props {
  data: DataDrivenTableDto;
  className?: string;
}

export function DataDrivenTable({ data, className }: Props) {
  const { sort, toggleSort, sortedData } = useSortableData(data.rows, undefined, compareCells);

  return (
    <Table sort={sort} onSortChange={toggleSort} className={className} size="small">
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
        {sortedData.map((row, index) => (
          <Table.Row key={index}>
            {data.columns.map((col) => (
              <Table.DataCell key={col.key} align={col.align}>
                {renderCell(row[col.key])}
              </Table.DataCell>
            ))}
          </Table.Row>
        ))}
      </Table.Body>
    </Table>
  );
}

function renderCell(cell?: DataElement): ReactNode {
  if (!cell) return "-";

  switch (cell.type) {
    case "text":
      return formatText(cell.value, cell.format || null);
    case "status":
      return <StatusTag variant={cell.variant}>{cell.value}</StatusTag>;
    case "periode":
      return `${formaterDato(cell.start)} - ${formaterDato(cell.slutt)}`;
    case "link":
      return <Lenke to={cell.href}>{cell.text}</Lenke>;
  }
}

function formatText(value: string, format: DataElementTextFormat | null): ReactNode {
  switch (format) {
    case DataElementTextFormat.DATE:
      return formaterDato(value);
    case DataElementTextFormat.NOK:
      return formaterNOK(Number(value));
    case DataElementTextFormat.NUMBER:
      return formaterTall(Number(value));
    case null:
      return value;
  }
}

function compareCells(aCell: DataElement, bCell: DataElement) {
  const aValue = getComparableValue(aCell);
  const bValue = getComparableValue(bCell);
  return compare(aValue, bValue);
}

function getComparableValue(cell: DataElement) {
  switch (cell.type) {
    case "text":
      return cell.value;
    case "status":
      return cell.value;
    case "periode":
      return cell.start;
    case "link":
      return cell.text;
  }
}
