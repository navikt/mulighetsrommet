import { DataElement, DataElementTextFormat } from "./types";
import { ReactNode } from "react";
import { formaterDato } from "../../utils/date";
import { compare, formaterNOK, formaterTall } from "../../utils/utils";

export function formatText(value: string, format: DataElementTextFormat | null): ReactNode {
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

export function compareDataElements(aCell: DataElement | null, bCell: DataElement | null) {
  const aValue = aCell ? getComparableValue(aCell) : null;
  const bValue = bCell ? getComparableValue(bCell) : null;
  return compare(aValue, bValue);
}

export function getComparableValue(element: DataElement) {
  switch (element.type) {
    case "DATA_ELEMENT_TEXT":
      return element.value;
    case "DATA_ELEMENT_STATUS":
      return element.value;
    case "DATA_ELEMENT_PERIODE":
      return element.start;
    case "DATA_ELEMENT_LINK":
      return element.text;
    case "DATA_ELEMENT_MATH_OPERATOR":
      return element.operator;
    case "DATA_ELEMENT_MULTI_LINK_MODAL":
      return element.modalContent.links[0].digest;
    case undefined:
      throw new Error(`Unrecognized data element: ${element}`);
  }
}
