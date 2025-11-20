import { DataElement, DataElementTextFormat } from "@api-client";

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
