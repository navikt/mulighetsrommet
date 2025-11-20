import { formaterDato } from "../../utils/date";
import { Lenke } from "../lenke/Lenke";
import { DataElementMathOperator } from "./DataElementMathOperator";
import { DataElementMultiLinkModal } from "./DataElementMultiLinkModal";
import { DataElementStatusTag } from "./DataElementStatusTag";
import { DataElement } from "./types";
import { formatText } from "./util";

export function getDataElement(element: DataElement) {
  switch (element.type) {
    case "no.nav.mulighetsrommet.model.DataElement.Text":
      return element.value ? formatText(element.value, element.format) : null;
    case "no.nav.mulighetsrommet.model.DataElement.Status":
      return <DataElementStatusTag {...element} />;
    case "no.nav.mulighetsrommet.model.DataElement.Periode":
      return `${formaterDato(element.start)} - ${formaterDato(element.slutt)}`;
    case "no.nav.mulighetsrommet.model.DataElement.Link":
      return (
        <Lenke to={element.href} className="whitespace-nowrap">
          {element.text}
        </Lenke>
      );
    case "no.nav.mulighetsrommet.model.DataElement.MathOperator":
      return <DataElementMathOperator operator={element.operator} />;
    case "no.nav.mulighetsrommet.model.DataElement.MultiLinkModal":
      return <DataElementMultiLinkModal data={element} />;
    case undefined:
      throw new Error(`Unrecognized data element: ${element}`);
  }
}
