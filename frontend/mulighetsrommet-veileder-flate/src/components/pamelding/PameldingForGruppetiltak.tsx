import { Button, Alert, Heading, VStack, BodyShort } from "@navikt/ds-react";
import {
  VeilederflateTiltaksgjennomforing,
  DeltakerStatusType,
  TiltakskodeArena,
  VeilederflateTiltakstype,
  DeltakerKort,
} from "mulighetsrommet-api-client";
import { ReactNode } from "react";
import { useGetTiltaksgjennomforingIdFraUrl } from "../../api/queries/useGetTiltaksgjennomforingIdFraUrl";
import { useHistorikkV2 } from "../../api/queries/useHistorikkV2";
import { resolveModiaRoute, ModiaRoute } from "../../apps/modia/ModiaRoute";
import { Link } from "react-router-dom";
import { Status } from "../../apps/modia/historikk/DeltakelseKort";

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
  const { aktive = [] } = deltakerHistorikk || {};
  const gjennomforingId = useGetTiltaksgjennomforingIdFraUrl();

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
    tiltakstypeStotterPamelding(tiltaksgjennomforing.tiltakstype) &&
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
          {tekster.tekst ? <BodyShort>{tekster.tekst}</BodyShort> : null}
          {vedtakRoute ? (
            <BodyShort>
              <Link to={vedtakRoute.href}>{tekster.lenketekst}</Link>
            </BodyShort>
          ) : null}
        </VStack>
      </Alert>
    );
  }
}

function utledTekster(deltakelse: DeltakerKort): {
  overskrift: string;
  tekst?: string;
  lenketekst: string;
  variant: "info" | "success";
} {
  switch (deltakelse.status.type) {
    case DeltakerStatusType.VENTER_PA_OPPSTART:
      return {
        overskrift: "Venter på oppstart",
        variant: "info",
        lenketekst: "Gå til vedtaket",
      };
    case DeltakerStatusType.DELTAR:
      return {
        overskrift: "Aktiv deltakelse",
        variant: "success",
        lenketekst: "Gå til vedtaket",
      };
    case DeltakerStatusType.UTKAST_TIL_PAMELDING:
      return {
        overskrift: "Utkast til påmelding",
        tekst: "Bruker har et utkast til påmelding",
        variant: "info",
        lenketekst: "Gå til vedtaket",
      };
    case DeltakerStatusType.KLADD:
      return {
        overskrift: "Kladd opprettet",
        tekst: "Kladd er ikke delt med bruker",
        lenketekst: "Gå til kladden",
        variant: "info",
      };
    default:
      throw new Error("Ukjent deltakerstatus");
  }
}

function tiltakstypeStotterPamelding(tiltakstype: VeilederflateTiltakstype): boolean {
  const whitelistTiltakstypeStotterPamelding = [
    TiltakskodeArena.ARBFORB,
    TiltakskodeArena.ARBRRHDAG,
    TiltakskodeArena.AVKLARAG,
    TiltakskodeArena.INDOPPFAG,
    TiltakskodeArena.VASV,
  ];
  return (
    !!tiltakstype.arenakode && whitelistTiltakstypeStotterPamelding.includes(tiltakstype.arenakode)
  );
}
