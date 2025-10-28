import { OpprettKravVeiviserNavigering, OpprettKravVeiviserSteg } from "@api-client";
import { Link as ReactRouterLink } from "react-router";
import { Button, HStack } from "@navikt/ds-react";
import { pathByOrgnr, pathBySteg } from "~/utils/navigation";

export const nesteStegFieldName = "nesteSteg";

interface OpprettKravVeiviserButtonsProps {
  orgnr: string;
  gjennomforingId: string;
  navigering: OpprettKravVeiviserNavigering;
  submitNeste?: boolean;
}

export function OpprettKravVeiviserButtons({
  orgnr,
  gjennomforingId,
  navigering,
  submitNeste,
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
        <LocalLinkButton
          steg={navigering.tilbake}
          text="Tilbake"
          orgnr={orgnr}
          gjennomforingId={gjennomforingId}
        />
      )}
      <NesteKnapp
        steg={navigering.neste}
        submitNeste={submitNeste}
        orgnr={orgnr}
        gjennomforingId={gjennomforingId}
      />
    </HStack>
  );
}

interface NesteKnappProps {
  steg: OpprettKravVeiviserSteg | null;
  orgnr: string;
  gjennomforingId: string;
  submitNeste?: boolean;
}

function NesteKnapp({ steg, orgnr, gjennomforingId, submitNeste }: NesteKnappProps) {
  if (!steg) {
    return null;
  }
  if (!submitNeste) {
    return (
      <LocalLinkButton
        steg={steg}
        text="Neste"
        orgnr={orgnr}
        gjennomforingId={gjennomforingId}
        isPrimary
      />
    );
  }
  return (
    <>
      <input name={nesteStegFieldName} value={steg.toString()} hidden readOnly />
      <Button type="submit" name="intent" value="submit">
        Neste
      </Button>
    </>
  );
}

interface LocalLinkButtonProps {
  steg: OpprettKravVeiviserSteg;
  text: string;
  orgnr: string;
  gjennomforingId: string;
  isPrimary?: boolean;
}

function LocalLinkButton({ steg, text, orgnr, gjennomforingId, isPrimary }: LocalLinkButtonProps) {
  return (
    <Button
      as={ReactRouterLink}
      variant={isPrimary ? "primary" : "tertiary"}
      className={isPrimary ? "justify-self-end" : ""}
      to={pathBySteg(steg, orgnr, gjennomforingId)}
    >
      {text}
    </Button>
  );
}
