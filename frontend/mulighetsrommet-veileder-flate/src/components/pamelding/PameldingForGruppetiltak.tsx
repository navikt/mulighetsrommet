import { useTiltakstyperSomStotterPameldingIModia } from "@/api/queries/useTiltakstyperSomSnartStotterPameldingIModia";
import { ModiaRoute, resolveModiaRoute } from "@/apps/modia/ModiaRoute";
import { useGetTiltaksgjennomforingIdFraUrl } from "@/hooks/useGetTiltaksgjennomforingIdFraUrl";
import {
  AmtDeltakerStatusType,
  DeltakerKortGruppetiltak,
  VeilederflateTiltakGruppe,
  VeilederflateTiltakstype,
} from "@mr/api-client";
import { Alert, BodyShort, Button, Heading, VStack } from "@navikt/ds-react";
import { ReactNode } from "react";
import styles from "./PameldingForGruppetiltak.module.scss";
import { useHentDeltakelseForGjennomforing } from "@/api/queries/useHentDeltakelseForGjennomforing";

interface PameldingProps {
  brukerHarRettPaaValgtTiltak: boolean;
  tiltak: VeilederflateTiltakGruppe;
}

export function PameldingForGruppetiltak({
  brukerHarRettPaaValgtTiltak,
  tiltak,
}: PameldingProps): ReactNode {
  const { data: aktivDeltakelse } = useHentDeltakelseForGjennomforing();
  const { data: stotterPameldingIModia = [] } = useTiltakstyperSomStotterPameldingIModia();
  const gjennomforingId = useGetTiltaksgjennomforingIdFraUrl();

  const skalVisePameldingslenke =
    brukerHarRettPaaValgtTiltak &&
    tiltakstypeStotterPamelding(stotterPameldingIModia, tiltak.tiltakstype) &&
    !aktivDeltakelse;

  const opprettDeltakelseRoute = resolveModiaRoute({
    route: ModiaRoute.ARBEIDSMARKEDSTILTAK_OPPRETT_DELTAKELSE,
    gjennomforingId,
  });

  let vedtakRoute = null;
  if (aktivDeltakelse) {
    vedtakRoute = resolveModiaRoute({
      route: ModiaRoute.ARBEIDSMARKEDSTILTAK_DELTAKELSE,
      deltakerId: aktivDeltakelse.id,
    });
  }

  if (!skalVisePameldingslenke && !aktivDeltakelse) {
    return null;
  }

  if (skalVisePameldingslenke) {
    return (
      <Button variant={"primary"} onClick={opprettDeltakelseRoute.navigate}>
        Start påmelding
      </Button>
    );
  } else if (aktivDeltakelse) {
    const tekster = utledTekster(aktivDeltakelse);
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
}

interface Tekst {
  overskrift: string;
  lenketekst: string;
  variant: "info" | "success" | "warning";
}

function utledTekster(deltakelse: DeltakerKortGruppetiltak): Tekst {
  switch (deltakelse.status.type) {
    case AmtDeltakerStatusType.VENTER_PA_OPPSTART:
      return {
        overskrift: "Venter på oppstart",
        variant: "info",
        lenketekst: "Les om brukerens deltakelse",
      };
    case AmtDeltakerStatusType.DELTAR:
      return {
        overskrift: "Brukeren deltar på tiltaket",
        variant: "success",
        lenketekst: "Les om brukerens deltakelse",
      };
    case AmtDeltakerStatusType.UTKAST_TIL_PAMELDING:
      return {
        overskrift: "Utkastet er delt og venter på godkjenning",
        variant: "info",
        lenketekst: "Gå til utkastet",
      };
    case AmtDeltakerStatusType.KLADD:
      return {
        overskrift: "Kladden er ikke delt",
        lenketekst: "Gå til kladden",
        variant: "warning",
      };
    default:
      throw new Error("Ukjent deltakerstatus");
  }
}

function tiltakstypeStotterPamelding(
  tiltakstyperSomStotterPamelding: string[],
  tiltakstype: VeilederflateTiltakstype,
): boolean {
  return (
    !!tiltakstype.tiltakskode && tiltakstyperSomStotterPamelding.includes(tiltakstype.tiltakskode)
  );
}
