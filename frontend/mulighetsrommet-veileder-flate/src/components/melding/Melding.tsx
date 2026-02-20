import { ReactNode } from "react";
import { InfoCard } from "@navikt/ds-react";
import {
  ExclamationmarkTriangleIcon,
  InformationSquareIcon,
  XMarkOctagonIcon,
} from "@navikt/aksel-icons";

interface MeldingProps {
  header: ReactNode;
  children?: ReactNode;
  variant: "info" | "warning" | "danger";
  utenMargin?: boolean;
}

export function Melding({ header, children, variant, utenMargin }: MeldingProps) {
  const ikon = () => {
    switch (variant) {
      case "info":
        return <InformationSquareIcon aria-hidden aria-label="Informasjons-ikon" />;
      case "warning":
        return <ExclamationmarkTriangleIcon aria-hidden aria-label="Varsel-ikon" />;
      case "danger":
        return <XMarkOctagonIcon aria-hidden aria-label="Feilmelding-ikon" />;
    }
  };

  const ariaLive = variant === "danger" ? "assertive" : "polite";

  return (
    <InfoCard data-color={variant} aria-live={ariaLive} className={utenMargin ? "mb-0" : "my-4"}>
      <InfoCard.Header icon={ikon()}>
        <InfoCard.Title>{header}</InfoCard.Title>
      </InfoCard.Header>
      <InfoCard.Content data-testid="melding-container">{children}</InfoCard.Content>
    </InfoCard>
  );
}
