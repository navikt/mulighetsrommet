import { ModiaRoute, resolveModiaRoute } from "@/apps/modia/ModiaRoute";
import { useGetTiltaksgjennomforingIdFraUrl } from "@/hooks/useGetTiltaksgjennomforingIdFraUrl";
import {
  DeltakelseGruppetiltak,
  GruppetiltakDeltakerStatus,
  Toggles,
  VeilederflateTiltakGruppe,
} from "@mr/api-client";
import { Alert, BodyShort, Button, Heading, VStack } from "@navikt/ds-react";
import { ReactNode } from "react";
import styles from "./PameldingForGruppetiltak.module.scss";
import { useHentDeltakelseForGjennomforing } from "@/api/queries/useHentDeltakelseForGjennomforing";
import { useFeatureToggle } from "@/api/feature-toggles";

interface PameldingProps {
  brukerHarRettPaaValgtTiltak: boolean;
  tiltak: VeilederflateTiltakGruppe;
}

export function PameldingForGruppetiltak({
  brukerHarRettPaaValgtTiltak,
  tiltak,
}: PameldingProps): ReactNode {
  const { data: brukerDeltarPaaValgtTiltak } = useHentDeltakelseForGjennomforing();
  const gjennomforingId = useGetTiltaksgjennomforingIdFraUrl();

  const tiltakskoder = tiltak.tiltakstype.tiltakskode ? [tiltak.tiltakstype.tiltakskode] : [];
  const { data: deltakelserErMigrert } = useFeatureToggle(
    Toggles.MULIGHETSROMMET_TILTAKSTYPE_MIGRERING_DELTAKER,
    tiltakskoder,
  );

  if (brukerDeltarPaaValgtTiltak) {
    const vedtakRoute = resolveModiaRoute({
      route: ModiaRoute.ARBEIDSMARKEDSTILTAK_DELTAKELSE,
      deltakerId: brukerDeltarPaaValgtTiltak.id,
    });

    const tekster = utledTekster(brukerDeltarPaaValgtTiltak);
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

  if (brukerHarRettPaaValgtTiltak && deltakelserErMigrert && tiltak.apentForInnsok) {
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
