import { ModiaRoute, resolveModiaRoute } from "@/apps/modia/ModiaRoute";
import { useTiltakIdFraUrl } from "@/hooks/useTiltakIdFraUrl";
import { DeltakerStatusType, VeilederflateTiltakGruppe } from "@api-client";
import { Alert, BodyShort, Button, Heading, HStack, VStack } from "@navikt/ds-react";
import { ReactNode } from "react";
import { PadlockLockedFillIcon } from "@navikt/aksel-icons";
import { useDeltakelse } from "@/api/queries/useDeltakelse";
import { Melding } from "../melding/Melding";

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

  if (deltakelse && deltakelse.pamelding) {
    const vedtakRoute = resolveModiaRoute({
      route: ModiaRoute.ARBEIDSMARKEDSTILTAK_DELTAKELSE,
      deltakerId: deltakelse.id,
    });

    const tekster = utledTekster(deltakelse.pamelding.status);
    return (
      <Alert variant={tekster.variant}>
        <Heading level={"2"} size="small">
          {tekster.overskrift}
        </Heading>
        <VStack gap="space-8">
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
        </VStack>
      </Alert>
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

function utledTekster(status: DeltakerStatusType): Tekst {
  switch (status) {
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
    case DeltakerStatusType.AVBRUTT:
    case DeltakerStatusType.AVBRUTT_UTKAST:
    case DeltakerStatusType.FEILREGISTRERT:
    case DeltakerStatusType.FULLFORT:
    case DeltakerStatusType.HAR_SLUTTET:
    case DeltakerStatusType.IKKE_AKTUELL:
    case DeltakerStatusType.PABEGYNT_REGISTRERING:
      throw new Error("Ukjent deltakerstatus");
  }
}
