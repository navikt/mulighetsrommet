import { ReactNode } from "react";
import styles from "./Feilmelding.module.scss";
import { Heading } from "@navikt/ds-react";
import classNames from "classnames";
import {
  ExclamationmarkTriangleFillIcon,
  InformationSquareFillIcon,
  XMarkOctagonFillIcon,
} from "@navikt/aksel-icons";

interface FeilmeldingProps {
  header: ReactNode;
  children?: ReactNode;
  ikonvariant?: string;
  utenMargin?: boolean;
}

export function Feilmelding({ header, children, ikonvariant, utenMargin }: FeilmeldingProps) {
  const ikon = () => {
    if (ikonvariant === "info") {
      return (
        <InformationSquareFillIcon aria-label="Informasjons-ikon" className="text-lightblue-700" />
      );
    } else if (ikonvariant === "warning") {
      return (
        <ExclamationmarkTriangleFillIcon aria-label="Varsel-ikon" className="text-orange-600" />
      );
    } else if (ikonvariant === "error") {
      return <XMarkOctagonFillIcon aria-label="Feilmelding-ikon" className="text-nav-red" />;
    }
  };

  const classNamesArray = utenMargin
    ? [styles.feilmelding_container]
    : [styles.feilmelding_container, styles.feilmelding_margin];

  return (
    <div
      data-testid="feilmelding-container"
      aria-live="assertive"
      className={classNames(...classNamesArray)}
    >
      {ikon()}
      <Heading level="4" size={"small"}>
        {header}
      </Heading>
      {children}
    </div>
  );
}
