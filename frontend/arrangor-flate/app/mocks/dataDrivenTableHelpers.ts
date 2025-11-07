import { DataElement, DataElementTextFormat } from "@api-client";

export function dataElementText(
  text: string,
  format: DataElementTextFormat | null = null,
): DataElement {
  return {
    type: "no.nav.mulighetsrommet.model.DataElement.Text",
    value: text,
    format: format,
  };
}

export function dataElementLink(text: string, href: string): DataElement {
  return {
    type: "no.nav.mulighetsrommet.model.DataElement.Link",
    text: text,
    href: href,
    digest: crypto.randomUUID().slice(0, 8),
  };
}
