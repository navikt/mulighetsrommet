import { DataElement, DataElementStatusVariant, DataElementTextFormat, Periode } from "@api-client";

export function dataElementText(
  text: string,
  format: DataElementTextFormat | null = null,
): DataElement {
  return {
    type: "DATA_ELEMENT_TEXT",
    value: text,
    format: format,
  };
}

export function dataElementLink(text: string, href: string): DataElement {
  return {
    type: "DATA_ELEMENT_LINK",
    text: text,
    href: href,
    digest: crypto.randomUUID().slice(0, 8),
  };
}

export function dataElementPeriode(periode: Periode): DataElement {
  return {
    type: "DATA_ELEMENT_PERIODE",
    start: periode.start,
    slutt: periode.slutt,
  };
}

export function dataElementAction(text: string, href: string): DataElement {
  return {
    type: "DATA_ELEMENT_LINK",
    text: text,
    href: href,
    digest: "123",
  };
}

export function dataElementStatus(value: string, variant: DataElementStatusVariant): DataElement {
  return {
    type: "DATA_ELEMENT_STATUS",
    value: value,
    variant: variant,
    description: null,
  };
}
