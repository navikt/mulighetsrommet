import { Alert, BodyShort, Button, Heading, VStack } from "@navikt/ds-react";
import {
  DeltakerKort,
  DeltakerStatusType,
  TiltakskodeArena,
  VeilederflateTiltaksgjennomforing,
  VeilederflateTiltakstype,
} from "mulighetsrommet-api-client";
import { ReactNode } from "react";
import { useGetTiltaksgjennomforingIdFraUrl } from "../../api/queries/useGetTiltaksgjennomforingIdFraUrl";
import { useHistorikkV2 } from "../../api/queries/useHistorikkV2";
import { useTiltakstyperSomStotterPameldingIModia } from "../../api/queries/useTiltakstyperSomStotterPameldingIModia";
import { ModiaRoute, resolveModiaRoute } from "../../apps/modia/ModiaRoute";
import styles from "./PameldingForGruppetiltak.module.scss";

interface PameldingProps {
  kanOppretteAvtaleForTiltak: boolean;
  brukerHarRettPaaValgtTiltak: boolean;
  tiltaksgjennomforing: VeilederflateTiltaksgjennomforing;
}

export function PameldingForGruppetiltak({
  kanOppretteAvtaleForTiltak,
  brukerHarRettPaaValgtTiltak,
  tiltaksgjennomforing,
}: PameldingProps): ReactNode {
  const { data: deltakerHistorikk } = useHistorikkV2();
  const { data: stotterPameldingIModia = [] } = useTiltakstyperSomStotterPameldingIModia();
  const gjennomforingId = useGetTiltaksgjennomforingIdFraUrl();

  const { aktive = [] } = deltakerHistorikk || {};
  const aktiveStatuser: DeltakerStatusType[] = [
    DeltakerStatusType.DELTAR,
    DeltakerStatusType.VENTER_PA_OPPSTART,
    DeltakerStatusType.UTKAST_TIL_PAMELDING,
    DeltakerStatusType.KLADD,
  ] as const;

  const aktivDeltakelse = aktive.find(
    (a) => a.deltakerlisteId === gjennomforingId && aktiveStatuser.includes(a.status.type),
  );

  const skalVisePameldingslenke =
    !kanOppretteAvtaleForTiltak &&
    brukerHarRettPaaValgtTiltak &&
    tiltakstypeStotterPamelding(stotterPameldingIModia, tiltaksgjennomforing.tiltakstype) &&
    !aktivDeltakelse;

  const opprettDeltakelseRoute = resolveModiaRoute({
    route: ModiaRoute.ARBEIDSMARKEDSTILTAK_OPPRETT_DELTAKELSE,
    gjennomforingId,
  });

  let vedtakRoute = null;
  if (aktivDeltakelse) {
    vedtakRoute = resolveModiaRoute({
      route: ModiaRoute.ARBEIDSMARKEDSTILTAK_DELTAKELSE,
      deltakerId: aktivDeltakelse.deltakerId,
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

function utledTekster(deltakelse: DeltakerKort): Tekst {
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
    default:
      throw new Error("Ukjent deltakerstatus");
  }
}

function tiltakstypeStotterPamelding(
  tiltakstyperSomStotterPamelding: string[],
  tiltakstype: VeilederflateTiltakstype,
): boolean {
  return !!tiltakstype.arenakode && tiltakstyperSomStotterPamelding.includes(tiltakstype.arenakode);
}
