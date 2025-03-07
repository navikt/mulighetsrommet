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

  return (
    <div
      data-testid="feilmelding-container"
      aria-live="assertive"
      className={`flex flex-col items-center justify-center bg-white w-[40rem] text-center gap-[0.5rem] px-[5rem] ${utenMargin ? "" : "mt-[2rem] mx-auto pt-[2rem] pb-[2rem] px-[5rem] min-h-[180px]"}`}
    >
      {ikon()}
      <Heading level="4" size={"small"}>
        {header}
      </Heading>
      {children}
    </div>
  );
}
