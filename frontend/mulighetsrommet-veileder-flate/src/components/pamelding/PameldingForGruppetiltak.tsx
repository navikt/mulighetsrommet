import { ModiaRoute, resolveModiaRoute } from "@/apps/modia/ModiaRoute";
import { useTiltakIdFraUrl } from "@/hooks/useTiltakIdFraUrl";
import { DeltakelseGruppetiltak, DeltakerStatusType, VeilederflateTiltakGruppe } from "@api-client";
import { Alert, BodyShort, Button, Heading, HStack, VStack } from "@navikt/ds-react";
import { ReactNode } from "react";
import { PadlockLockedFillIcon } from "@navikt/aksel-icons";
import { useDeltakelse } from "@/api/queries/useDeltakelse";

interface PameldingProps {
  brukerHarRettPaaValgtTiltak: boolean;
  tiltak: VeilederflateTiltakGruppe;
}

export function PameldingForGruppetiltak({
  brukerHarRettPaaValgtTiltak,
  tiltak,
}: PameldingProps): ReactNode {
  const { data: deltakelse } = useDeltakelse();
  const gjennomforingId = useTiltakIdFraUrl();

  if (deltakelse) {
    const vedtakRoute = resolveModiaRoute({
      route: ModiaRoute.ARBEIDSMARKEDSTILTAK_DELTAKELSE,
      deltakerId: deltakelse.id,
    });

    const tekster = utledTekster(deltakelse);
    return (
      <Alert variant={tekster.variant}>
        <Heading level={"2"} size="small">
          {tekster.overskrift}
        </Heading>
        <VStack gap="2">
          {vedtakRoute ? (
            <BodyShort>
              <Button
                role="link"
                variant="tertiary"
                className="underline"
                size="xsmall"
                onClick={vedtakRoute.navigate}
              >
                {tekster.lenketekst}
              </Button>
            </BodyShort>
          ) : null}
        </VStack>
      </Alert>
    );
  }

  if (!tiltak.apentForPamelding) {
    return (
      <Alert variant="info">
        <HStack align="center" gap="1">
          Tiltaket er stengt for påmelding <PadlockLockedFillIcon />
        </HStack>
      </Alert>
    );
  }

  if (brukerHarRettPaaValgtTiltak && tiltak.apentForPamelding) {
    const opprettDeltakelseRoute = resolveModiaRoute({
      route: ModiaRoute.ARBEIDSMARKEDSTILTAK_OPPRETT_DELTAKELSE,
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

interface Tekst {
  overskrift: string;
  lenketekst: string;
  variant: "info" | "success" | "warning";
}

function utledTekster(deltakelse: DeltakelseGruppetiltak): Tekst {
  switch (deltakelse.status.type) {
    case DeltakerStatusType.VENTER_PA_OPPSTART:
      return {
        overskrift: "Venter på oppstart",
        variant: "info",
        lenketekst: "Les om brukerens deltakelse",
      };
    case DeltakerStatusType.DELTAR:
      return {
        overskrift: "Brukeren deltar på tiltaket",
        variant: "success",
        lenketekst: "Les om brukerens deltakelse",
      };
    case DeltakerStatusType.UTKAST_TIL_PAMELDING:
      return {
        overskrift: "Utkastet er delt og venter på godkjenning",
        variant: "info",
        lenketekst: "Gå til utkastet",
      };
    case DeltakerStatusType.KLADD:
      return {
        overskrift: "Kladden er ikke delt",
        lenketekst: "Gå til kladden",
        variant: "warning",
      };
    case DeltakerStatusType.SOKT_INN:
      return {
        overskrift: "Brukeren er søkt inn",
        lenketekst: "Les om brukerens deltakelse",
        variant: "info",
      };
    case DeltakerStatusType.VENTELISTE:
      return {
        overskrift: "Brukeren er på venteliste",
        lenketekst: "Les om brukerens deltakelse",
        variant: "info",
      };
    case DeltakerStatusType.VURDERES:
      return {
        overskrift: "Brukeren er søkt inn",
        lenketekst: "Les om brukerens deltakelse",
        variant: "info",
      };
    default:
      throw new Error("Ukjent deltakerstatus");
  }
}
