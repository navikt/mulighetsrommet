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
    case "no.nav.mulighetsrommet.model.DataElement.Text":
      return element.value;
    case "no.nav.mulighetsrommet.model.DataElement.Status":
      return element.value;
    case "no.nav.mulighetsrommet.model.DataElement.Periode":
      return element.start;
    case "no.nav.mulighetsrommet.model.DataElement.Link":
      return element.text;
    case "no.nav.mulighetsrommet.model.DataElement.MathOperator":
      return element.operator;
    case "no.nav.mulighetsrommet.model.DataElement.MultiLinkModal":
      return element.modalContent.links[0].digest;
    case undefined:
      throw new Error(`Unrecognized data element: ${element}`);
  }
}
