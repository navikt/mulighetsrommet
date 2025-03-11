import { Lenke } from "@mr/frontend-common/components/lenke/Lenke";
import { BodyShort } from "@navikt/ds-react";
import { ChevronLeftIcon } from "@navikt/aksel-icons";

interface TilbakeknappProps {
  tilbakelenke: string;
  tekst?: string;
}

export function Tilbakeknapp({ tilbakelenke, tekst = "Tilbake" }: TilbakeknappProps) {
  return (
    <Lenke className="flex items-center gap-0.5 my-2" to={tilbakelenke}>
      <ChevronLeftIcon aria-label="Tilbakeknapp" />
      <BodyShort size="small">{tekst}</BodyShort>
    </Lenke>
  );
}
