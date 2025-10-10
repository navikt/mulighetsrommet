import { OpprettKravVeiviserNavigering } from "@api-client";
import { Link as ReactRouterLink } from "react-router";
import { Button, HStack } from "@navikt/ds-react";
import { pathByOrgnr, pathBySteg } from "~/utils/navigation";

interface OpprettKravVeiviserButtonsProps {
  orgnr: string;
  gjennomforingId: string;
  navigering: OpprettKravVeiviserNavigering;
}

export function OpprettKravVeiviserButtons({
  orgnr,
  gjennomforingId,
  navigering,
}: OpprettKravVeiviserButtonsProps) {
  if (!navigering.tilbake && !navigering.neste) {
    return (
      <HStack gap="4">
        <Button
          as={ReactRouterLink}
          type="button"
          variant="tertiary"
          to={pathByOrgnr(orgnr).opprettKrav.tiltaksOversikt}
        >
          Avbryt
        </Button>
      </HStack>
    );
  }
  return (
    <HStack gap="4">
      {navigering.tilbake && (
        <Button
          as={ReactRouterLink}
          type="button"
          variant="tertiary"
          to={pathBySteg(navigering.tilbake, orgnr, gjennomforingId)}
        >
          Tilbake
        </Button>
      )}
      {navigering.neste && (
        <Button
          as={ReactRouterLink}
          className="justify-self-end"
          to={pathBySteg(navigering.neste, orgnr, gjennomforingId)}
        >
          Neste
        </Button>
      )}
    </HStack>
  );
}
