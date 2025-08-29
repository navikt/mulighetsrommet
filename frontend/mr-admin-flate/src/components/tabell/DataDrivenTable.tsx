import { BodyLong, Table, TableProps, TagProps, VStack } from "@navikt/ds-react";
import {
  DataDrivenTableDto,
  DataElement,
  DataElementMathOperatorType,
  DataElementStatusVariant,
  DataElementTextFormat,
  LabeledDataElement,
  LabeledDataElementType,
} from "@mr/api-client-v2";
import { compare, formaterNOK, formaterTall } from "@mr/frontend-common/utils/utils";
import { StatusTag, useSortableData } from "@mr/frontend-common";
import { formaterDato } from "@mr/frontend-common/utils/date";
import { Lenke } from "@mr/frontend-common/components/lenke/Lenke";
import { ReactNode } from "react";
import { Metadata, MetadataHorisontal } from "../detaljside/Metadata";

interface Props {
  data: DataDrivenTableDto;
  className?: string;
  size?: TableProps["size"];
}

export function DataDrivenTable({ data, className, size }: Props) {
  const { sort, toggleSort, sortedData } = useSortableData(data.rows, undefined, compareCells);

  return (
    <Table sort={sort} onSortChange={toggleSort} className={className} size={size}>
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
            {data.columns.map((col) => {
              const cell = row[col.key];
              return (
                <Table.DataCell key={col.key} align={col.align}>
                  {cell ? renderCell(cell) : null}
                </Table.DataCell>
              );
            })}
          </Table.Row>
        ))}
      </Table.Body>
    </Table>
  );
}

export function renderCell(cell: DataElement): ReactNode {
  switch (cell.type) {
    case "text":
      return cell.value ? formatText(cell.value, cell.format) : null;
    case "status":
      return <DataElementStatusTag {...cell} />;
    case "periode":
      return `${formaterDato(cell.start)} - ${formaterDato(cell.slutt)}`;
    case "link":
      return <Lenke to={cell.href}>{cell.text}</Lenke>;
    case "math-operator":
      return <MathOperator operator={cell.operator} />;
  }
}

function MathOperator({ operator }: { operator: DataElementMathOperatorType }) {
  switch (operator) {
    case DataElementMathOperatorType.PLUS:
      return "+";
    case DataElementMathOperatorType.MINUS:
      return "−";
    case DataElementMathOperatorType.MULTIPLY:
      return "×";
    case DataElementMathOperatorType.DIVIDE:
      return "÷";
    case DataElementMathOperatorType.EQUALS:
      return "=";
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

function compareCells(aCell: DataElement | null, bCell: DataElement | null) {
  const aValue = aCell ? getComparableValue(aCell) : null;
  const bValue = bCell ? getComparableValue(bCell) : null;
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
    case "math-operator":
      return cell.operator;
  }
}

interface DataElementStatusTagProps {
  value: string;
  variant: DataElementStatusVariant;
}

function DataElementStatusTag(props: DataElementStatusTagProps) {
  const { variant, className } = getStatusTagStyles(props.variant);
  return (
    <StatusTag variant={variant} className={className}>
      {props.value}
    </StatusTag>
  );
}

function getStatusTagStyles(variant: DataElementStatusVariant): {
  variant: TagProps["variant"];
  className?: string;
} {
  switch (variant) {
    case DataElementStatusVariant.NEUTRAL:
      return { variant: "neutral" };
    case DataElementStatusVariant.SUCCESS:
      return { variant: "success" };
    case DataElementStatusVariant.WARNING:
      return { variant: "warning" };
    case DataElementStatusVariant.ERROR:
      return { variant: "error" };
    case DataElementStatusVariant.ERROR_BORDER:
      return {
        variant: "neutral",
        className: "bg-white border-[color:var(--a-text-danger)]",
      };
    case DataElementStatusVariant.ERROR_BORDER_STRIKETHROUGH:
      return {
        variant: "neutral",
        className:
          "bg-white text-[color:var(--a-text-danger)] border-[color:var(--a-text-danger)] line-through",
      };
  }
}

export function DataDetails({ entries }: { entries: LabeledDataElement[] }) {
  return (
    <VStack gap="4">
      {entries.map((entry) => (
        <LabaledDataElement key={entry.label} {...entry} />
      ))}
    </VStack>
  );
}

export function LabaledDataElement(props: LabeledDataElement) {
  const label = props.label;
  const value = props.value ? renderCell(props.value) : null;
  const valueOrFallback = value || "-";

  switch (props.type) {
    case LabeledDataElementType.INLINE:
      return <MetadataHorisontal header={label} value={valueOrFallback} />;
    case LabeledDataElementType.MULTILINE:
      return (
        <Metadata
          header={label}
          value={<BodyLong className="whitespace-pre-line">{valueOrFallback}</BodyLong>}
        />
      );
  }
}
