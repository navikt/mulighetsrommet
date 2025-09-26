import { type DataElement, DataElementTextFormat } from "@api-client";
import { Lenke } from "@mr/frontend-common/components/lenke/Lenke";
import { formaterDato } from "@mr/frontend-common/utils/date";

import { DataElementMathOperator } from "./DataElementMathOperator";
import { compare, formaterNOK, formaterTall } from "@mr/frontend-common/utils/utils";
import { ReactNode } from "react";
import { DataElementStatusTag } from "./DataElementStatusTag";

export function getDataElement(element: DataElement) {
  switch (element.type) {
    case "no.nav.mulighetsrommet.model.DataElement.Text":
      return element.value ? formatText(element.value, element.format) : null;
    case "no.nav.mulighetsrommet.model.DataElement.Status":
      return <DataElementStatusTag {...element} />;
    case "no.nav.mulighetsrommet.model.DataElement.Periode":
      return `${formaterDato(element.start)} - ${formaterDato(element.slutt)}`;
    case "no.nav.mulighetsrommet.model.DataElement.Link":
      return <Lenke to={element.href}>{element.text}</Lenke>;
    case "no.nav.mulighetsrommet.model.DataElement.MathOperator":
      return <DataElementMathOperator operator={element.operator} />;
    case undefined:
      throw new Error(`Unrecognized data element: ${element}`);
  }
}

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
    case undefined:
      throw new Error(`Unrecognized data element: ${element}`);
  }
}
