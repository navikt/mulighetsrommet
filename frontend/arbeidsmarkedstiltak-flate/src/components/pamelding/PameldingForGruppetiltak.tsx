import { ModiaRoute, resolveModiaRoute } from "@/apps/modia/ModiaRoute";
import { useTiltakIdFraUrl } from "@/hooks/useTiltakIdFraUrl";
import { Tiltaksadministrasjon, VeilederflateTiltakGruppe } from "@arbeidsmarkedstiltak/api-client";
import { Button, HStack } from "@navikt/ds-react";
import { ReactNode } from "react";
import { PadlockLockedFillIcon } from "@navikt/aksel-icons";
import { useAktiveDeltakelser } from "@/api/queries/useDeltakelse";
import { Melding } from "../melding/Melding";
import { InfoMeldingDeltakelse } from "./PameldingDeltakelseAlert";

interface PameldingProps {
  brukerHarRettPaaValgtTiltak: boolean;
  tiltak: VeilederflateTiltakGruppe;
}

export function PameldingForGruppetiltak({
  brukerHarRettPaaValgtTiltak,
  tiltak,
}: PameldingProps): ReactNode {
  const { data: deltakelser } = useAktiveDeltakelser();
  const gjennomforingId = useTiltakIdFraUrl();

  if (deltakelser && deltakelser.length > 0) {
    return (
      <>
        {deltakelser
          .filter((d): d is Tiltaksadministrasjon => d.type === "TILTAKSADMINISTRASJON")
          .map((deltakelse) => (
            <InfoMeldingDeltakelse key={deltakelse.id} deltakelse={deltakelse} />
          ))}
      </>
    );
  }

  if (!tiltak.apentForPamelding) {
    return (
      <Melding header="Stengt for påmelding" variant="info">
        <HStack align="center" gap="space-4">
          Tiltaket er stengt for påmelding <PadlockLockedFillIcon />
        </HStack>
      </Melding>
    );
  }

  if (brukerHarRettPaaValgtTiltak) {
    const opprettDeltakelseRoute = resolveModiaRoute({
      route: ModiaRoute.ARBEIDSMARKEDSTILTAK_DELTAKELSE_PAMELDING,
      gjennomforingId,
    });

    return (
      <Button variant={"primary"} onClick={opprettDeltakelseRoute.navigate}>
        Start påmelding
      </Button>
    );
  }

  return null;
}
