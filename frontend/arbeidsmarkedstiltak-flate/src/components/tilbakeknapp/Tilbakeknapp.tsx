import { ChevronLeftIcon } from "@navikt/aksel-icons";
import { BodyShort, Link } from "@navikt/ds-react";
import { Link as ReactRouterLink } from "react-router";

interface TilbakeknappProps {
  tilbakelenke: string;
  tekst?: string;
}

export function Tilbakeknapp({ tilbakelenke, tekst = "Tilbake" }: TilbakeknappProps) {
  return (
    <Link as={ReactRouterLink} to={tilbakelenke}>
      <ChevronLeftIcon aria-label="Tilbakeknapp" />
      <BodyShort size="small">{tekst}</BodyShort>
    </Link>
  );
}
