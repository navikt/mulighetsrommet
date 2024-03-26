import { ReactNode } from "react";
import styles from "./Feilmelding.module.scss";
import svgStyle from "../../App.module.scss";
import { BodyShort, Heading } from "@navikt/ds-react";
import classNames from "classnames";
import {
  ExclamationmarkTriangleFillIcon,
  InformationSquareFillIcon,
  XMarkOctagonFillIcon,
} from "@navikt/aksel-icons";

interface FeilmeldingProps {
  header: ReactNode;
  beskrivelse?: ReactNode;
  children?: ReactNode;
  ikonvariant?: string;
  utenMargin?: boolean;
}

export const Feilmelding = ({
  header,
  beskrivelse,
  children,
  ikonvariant,
  utenMargin,
}: FeilmeldingProps) => {
  const ikon = () => {
    if (ikonvariant === "info") {
      return (
        <InformationSquareFillIcon aria-label="Informasjons-ikon" className={svgStyle.svg_info} />
      );
    } else if (ikonvariant === "warning") {
      return (
        <ExclamationmarkTriangleFillIcon
          aria-label="Varsel-ikon"
          className={svgStyle.svg_warning}
        />
      );
    } else if (ikonvariant === "error") {
      return <XMarkOctagonFillIcon aria-label="Feilmelding-ikon" className={svgStyle.svg_error} />;
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
      <BodyShort as="div" size={"small"} className={styles.beskrivelse}>
        {beskrivelse}
      </BodyShort>
      {children}
    </div>
  );
};
