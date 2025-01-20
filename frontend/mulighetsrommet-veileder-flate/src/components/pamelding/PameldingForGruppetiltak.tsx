import { ModiaRoute, resolveModiaRoute } from "@/apps/modia/ModiaRoute";
import { useGetTiltakIdFraUrl } from "@/hooks/useGetTiltaksgjennomforingIdFraUrl";
import {
  DeltakelseGruppetiltak,
  GruppetiltakDeltakerStatus,
  Toggles,
  VeilederflateTiltakGruppe,
} from "@mr/api-client-v2";
import { Alert, BodyShort, Button, Heading, HStack, VStack } from "@navikt/ds-react";
import { ReactNode } from "react";
import styles from "./PameldingForGruppetiltak.module.scss";
import { useFeatureToggle } from "@/api/feature-toggles";
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
  const gjennomforingId = useGetTiltakIdFraUrl();

  const tiltakskoder = tiltak.tiltakstype.tiltakskode ? [tiltak.tiltakstype.tiltakskode] : [];
  const { data: deltakelserErMigrert } = useFeatureToggle(
    Toggles.MULIGHETSROMMET_TILTAKSTYPE_MIGRERING_DELTAKER,
    tiltakskoder,
  );

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
                className={styles.knapp_som_lenke}
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

  if (!tiltak.apentForPamelding && deltakelserErMigrert) {
    return (
      <Alert variant="info">
        <HStack align="center" gap="1">
          Tiltaket er stengt for påmelding <PadlockLockedFillIcon />
        </HStack>
      </Alert>
    );
  }

  if (brukerHarRettPaaValgtTiltak && deltakelserErMigrert && tiltak.apentForPamelding) {
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
    case GruppetiltakDeltakerStatus.VENTER_PA_OPPSTART:
      return {
        overskrift: "Venter på oppstart",
        variant: "info",
        lenketekst: "Les om brukerens deltakelse",
      };
    case GruppetiltakDeltakerStatus.DELTAR:
      return {
        overskrift: "Brukeren deltar på tiltaket",
        variant: "success",
        lenketekst: "Les om brukerens deltakelse",
      };
    case GruppetiltakDeltakerStatus.UTKAST_TIL_PAMELDING:
      return {
        overskrift: "Utkastet er delt og venter på godkjenning",
        variant: "info",
        lenketekst: "Gå til utkastet",
      };
    case GruppetiltakDeltakerStatus.KLADD:
      return {
        overskrift: "Kladden er ikke delt",
        lenketekst: "Gå til kladden",
        variant: "warning",
      };
    default:
      throw new Error("Ukjent deltakerstatus");
  }
}
