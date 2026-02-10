import { ReactNode } from "react";
import { Heading } from "@navikt/ds-react";
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
        <InformationSquareFillIcon
          aria-label="Informasjons-ikon"
          className="bg-ax-text-accent-decoration"
        />
      );
    } else if (ikonvariant === "warning") {
      return (
        <ExclamationmarkTriangleFillIcon
          aria-label="Varsel-ikon"
          className="bg-ax-text-warning-decoration"
        />
      );
    } else if (ikonvariant === "error") {
      return (
        <XMarkOctagonFillIcon
          aria-label="Feilmelding-ikon"
          className="bg-ax-text-danger-decoration"
        />
      );
    }
  };

  return (
    <div
      data-testid="feilmelding-container"
      aria-live="assertive"
      className={`flex flex-col items-center justify-center bg-ax-bg-default w-160 text-center gap-2 px-20 ${utenMargin ? "" : "mt-8 mx-auto pt-8 pb-8 px-20 min-h-45"}`}
    >
      {ikon()}
      <Heading level="4" size={"small"}>
        {header}
      </Heading>
      {children}
    </div>
  );
}
