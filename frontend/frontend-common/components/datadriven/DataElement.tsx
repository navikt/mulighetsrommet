import { formaterDato } from "../../utils/date";
import { DataElementMoneyAmount } from "./DataElementMoneyAmount";
import { DataElementMathOperator } from "./DataElementMathOperator";
import { DataElementMultiLinkModal } from "./DataElementMultiLinkModal";
import { DataElementStatusTag } from "./DataElementStatusTag";
import { DataElement } from "./types";
import { formatText } from "./util";
import { Link } from "@navikt/ds-react/Link";
import { Link as ReactRouterLink } from "react-router";

export function getDataElement(element: DataElement) {
  switch (element.type) {
    case "DATA_ELEMENT_TEXT":
      return element.value ? formatText(element.value, element.format) : null;
    case "DATA_ELEMENT_STATUS":
      return <DataElementStatusTag {...element} />;
    case "DATA_ELEMENT_PERIODE":
      return `${formaterDato(element.start)} - ${formaterDato(element.slutt)}`;
    case "DATA_ELEMENT_LINK":
      return (
        <Link as={ReactRouterLink} to={element.href} className="whitespace-nowrap">
          {element.text}
        </Link>
      );
    case "DATA_ELEMENT_MATH_OPERATOR":
      return <DataElementMathOperator operator={element.operator} />;
    case "DATA_ELEMENT_MULTI_LINK_MODAL":
      return <DataElementMultiLinkModal data={element} />;
    case "DATA_ELEMENT_MONEY_AMOUNT":
      return <DataElementMoneyAmount data={element} />;
    case undefined:
      throw new Error(`Unrecognized data element: ${element}`);
  }
}
