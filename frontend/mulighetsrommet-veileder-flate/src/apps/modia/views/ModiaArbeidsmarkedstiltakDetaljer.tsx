import { InlineErrorBoundary } from "@/ErrorBoundary";
import { useFeatureToggle } from "@/api/feature-toggles";
import { useGetTiltaksgjennomforingIdFraUrl } from "@/api/queries/useGetTiltaksgjennomforingIdFraUrl";
import { useTiltaksgjennomforingById } from "@/api/queries/useTiltaksgjennomforingById";
import { ModiaRoute, resolveModiaRoute } from "@/apps/modia/ModiaRoute";
import { DelMedBruker } from "@/apps/modia/delMedBruker/DelMedBruker";
import { useHentBrukerdata } from "@/apps/modia/hooks/useHentBrukerdata";
import { useHentDeltMedBrukerStatus } from "@/apps/modia/hooks/useHentDeltMedbrukerStatus";
import { useHentVeilederdata } from "@/apps/modia/hooks/useHentVeilederdata";
import { useModiaContext } from "@/apps/modia/hooks/useModiaContext";
import { BrukerKvalifisererIkkeVarsel } from "@/apps/modia/varsler/BrukerKvalifisererIkkeVarsel";
import { TiltakLoader } from "@/components/TiltakLoader";
import { DetaljerJoyride } from "@/components/joyride/DetaljerJoyride";
import { OpprettAvtaleJoyride } from "@/components/joyride/OpprettAvtaleJoyride";
import { PersonvernContainer } from "@/components/personvern/PersonvernContainer";
import { LenkeListe } from "@/components/sidemeny/Lenker";
import { Tilbakeknapp } from "@/components/tilbakeknapp/Tilbakeknapp";
import { paginationAtom } from "@/core/atoms";
import { isProduction } from "@/environment";
import { ViewTiltaksgjennomforingDetaljer } from "@/layouts/ViewTiltaksgjennomforingDetaljer";
import { Chat2Icon } from "@navikt/aksel-icons";
import { Alert, BodyShort, Button, Heading, Link, VStack } from "@navikt/ds-react";
import { useAtomValue } from "jotai";
import {
  Bruker,
  NavVeileder,
  TiltakskodeArena,
  Toggles,
  VeilederflateTiltaksgjennomforing,
  VeilederflateTiltakstype,
} from "mulighetsrommet-api-client";
import { useTitle } from "mulighetsrommet-frontend-common";
import { useHistorikkV2 } from "../../../api/queries/useHistorikkV2";
import { ReactNode } from "react";

export function ModiaArbeidsmarkedstiltakDetaljer() {
  const { fnr } = useModiaContext();
  const id = useGetTiltaksgjennomforingIdFraUrl();
  const { delMedBrukerInfo, lagreVeilederHarDeltTiltakMedBruker } = useHentDeltMedBrukerStatus(
    fnr,
    id,
  );

  const {
    data: veilederdata,
    isPending: isPendingVeilederdata,
    isError: isErrorVeilederdata,
  } = useHentVeilederdata();
  const {
    data: brukerdata,
    isPending: isPendingBrukerdata,
    isError: isErrorBrukerdata,
  } = useHentBrukerdata();
  const {
    data: tiltaksgjennomforing,
    isPending: isPendingTiltak,
    isError,
  } = useTiltaksgjennomforingById();

  useTitle(
    `Arbeidsmarkedstiltak - Detaljer ${
      tiltaksgjennomforing?.navn ? `- ${tiltaksgjennomforing.navn}` : null
    }`,
  );

  const pagination = useAtomValue(paginationAtom);

  if (isPendingTiltak || isPendingVeilederdata || isPendingBrukerdata) {
    return <TiltakLoader />;
  }

  if (isError || isErrorVeilederdata || isErrorBrukerdata) {
    return <Alert variant="error">Det har skjedd en feil</Alert>;
  }

  const tiltakstype = tiltaksgjennomforing.tiltakstype;
  const kanOppretteAvtaleForTiltak =
    isIndividueltTiltak(tiltakstype) && brukerdata.erUnderOppfolging;
  const brukerHarRettPaaValgtTiltak = harBrukerRettPaaValgtTiltak(brukerdata, tiltakstype);

  const dialogRoute = delMedBrukerInfo
    ? resolveModiaRoute({
        route: ModiaRoute.DIALOG,
        dialogId: delMedBrukerInfo.dialogId,
      })
    : null;

  return (
    <>
      <BrukerKvalifisererIkkeVarsel
        brukerdata={brukerdata}
        brukerHarRettPaaTiltak={brukerHarRettPaaValgtTiltak}
        brukerErUnderOppfolging={brukerdata.erUnderOppfolging}
      />
      <ViewTiltaksgjennomforingDetaljer
        tiltaksgjennomforing={tiltaksgjennomforing}
        knapperad={
          <>
            <Tilbakeknapp
              tilbakelenke={`/arbeidsmarkedstiltak/oversikt#pagination=${encodeURIComponent(
                JSON.stringify({ ...pagination }),
              )}`}
              tekst="Gå til oversikt over aktuelle tiltak"
            />
            <div>
              <DetaljerJoyride opprettAvtale={kanOppretteAvtaleForTiltak} />
              {kanOppretteAvtaleForTiltak ? (
                <OpprettAvtaleJoyride opprettAvtale={kanOppretteAvtaleForTiltak} />
              ) : null}
            </div>
          </>
        }
        brukerActions={
          <>
            {kanOppretteAvtaleForTiltak && (
              <Button
                onClick={() => {
                  const url = lenkeTilOpprettAvtale();
                  window.open(url, "_blank");
                }}
                variant="primary"
                aria-label="Opprett avtale"
                data-testid="opprettavtaleknapp"
                disabled={!brukerHarRettPaaValgtTiltak}
              >
                Opprett avtale
              </Button>
            )}

            <Pamelding
              kanOppretteAvtaleForTiltak={kanOppretteAvtaleForTiltak}
              brukerHarRettPaaValgtTiltak={brukerHarRettPaaValgtTiltak}
              tiltaksgjennomforing={tiltaksgjennomforing}
            />

            {brukerdata.erUnderOppfolging ? (
              <DelMedBruker
                delMedBrukerInfo={delMedBrukerInfo ?? undefined}
                veiledernavn={resolveName(veilederdata)}
                tiltaksgjennomforing={tiltaksgjennomforing}
                bruker={brukerdata}
                lagreVeilederHarDeltTiltakMedBruker={lagreVeilederHarDeltTiltakMedBruker}
              />
            ) : null}

            {!brukerdata?.manuellStatus && (
              <Alert
                title="Vi kunne ikke opprette kontakt med KRR og vet derfor ikke om brukeren har reservert seg mot elektronisk kommunikasjon"
                key="alert-innsatsgruppe"
                data-testid="alert-innsatsgruppe"
                size="small"
                variant="error"
              >
                Vi kunne ikke opprette kontakt med KRR og vet derfor ikke om brukeren har reservert
                seg mot elektronisk kommunikasjon
              </Alert>
            )}

            {dialogRoute && brukerdata.erUnderOppfolging && (
              <Button size="small" variant="tertiary" onClick={dialogRoute.navigate}>
                Åpne i dialogen
                <Chat2Icon aria-label="Åpne i dialogen" />
              </Button>
            )}

            {tiltaksgjennomforing && tiltaksgjennomforing?.personvernBekreftet ? (
              <InlineErrorBoundary>
                <PersonvernContainer tiltaksgjennomforing={tiltaksgjennomforing} />
              </InlineErrorBoundary>
            ) : null}

            <LenkeListe lenker={tiltaksgjennomforing.faneinnhold?.lenker} />
          </>
        }
      />
    </>
  );
}

const whiteListOpprettAvtaleKnapp: TiltakskodeArena[] = [
  TiltakskodeArena.MIDLONTIL,
  TiltakskodeArena.ARBTREN,
  TiltakskodeArena.VARLONTIL,
  TiltakskodeArena.MENTOR,
  TiltakskodeArena.INKLUTILS,
  TiltakskodeArena.TILSJOBB,
];

function resolveName(ansatt: NavVeileder) {
  return [ansatt.fornavn, ansatt.etternavn].filter((part) => part !== "").join(" ");
}

function isIndividueltTiltak(tiltakstype: VeilederflateTiltakstype): boolean {
  return (
    tiltakstype.arenakode !== undefined &&
    whiteListOpprettAvtaleKnapp.includes(tiltakstype.arenakode)
  );
}

function lenkeTilOpprettAvtale(): string {
  const baseUrl = isProduction
    ? "https://tiltaksgjennomforing.intern.nav.no"
    : "https://tiltaksgjennomforing.intern.dev.nav.no";
  return `${baseUrl}/tiltaksgjennomforing/opprett-avtale`;
}

function harBrukerRettPaaValgtTiltak(
  bruker: Bruker,
  tiltakstype: VeilederflateTiltakstype,
): boolean {
  if (!bruker.erUnderOppfolging || !bruker.innsatsgruppe) {
    return false;
  }

  return (tiltakstype.innsatsgrupper ?? []).includes(bruker.innsatsgruppe);
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

interface PameldingProps {
  kanOppretteAvtaleForTiltak: boolean;
  brukerHarRettPaaValgtTiltak: boolean;
  tiltaksgjennomforing: VeilederflateTiltaksgjennomforing;
}

function Pamelding({
  kanOppretteAvtaleForTiltak,
  brukerHarRettPaaValgtTiltak,
  tiltaksgjennomforing,
}: PameldingProps): ReactNode {
  const { data: enableDeltakerRegistrering } = useFeatureToggle(
    Toggles.MULIGHETSROMMET_VEILEDERFLATE_VIS_DELTAKER_REGISTRERING,
  );
  const { data: deltakerHistorikk } = useHistorikkV2();
  const { aktive = [] } = deltakerHistorikk || {};
  const gjennomforingId = useGetTiltaksgjennomforingIdFraUrl();

  const harAktivDeltakelse = aktive.find((a) => a.deltakerlisteId === gjennomforingId);

  if (!enableDeltakerRegistrering) return null;

  const skalVisePameldingslenke =
    enableDeltakerRegistrering &&
    !kanOppretteAvtaleForTiltak &&
    brukerHarRettPaaValgtTiltak &&
    tiltakstypeStotterPamelding(tiltaksgjennomforing.tiltakstype) &&
    !harAktivDeltakelse;

  const opprettDeltakelseRoute = resolveModiaRoute({
    route: ModiaRoute.ARBEIDSMARKEDSTILTAK_OPPRETT_DELTAKELSE,
    gjennomforingId,
  });

  let vedtakRoute = null;
  if (harAktivDeltakelse) {
    vedtakRoute = resolveModiaRoute({
      route: ModiaRoute.ARBEIDSMARKEDSTILTAK_DELTAKELSE,
      deltakerId: harAktivDeltakelse.deltakerId,
    });
  }

  if (skalVisePameldingslenke) {
    return (
      <Button variant={"primary"} onClick={opprettDeltakelseRoute.navigate}>
        Start påmelding
      </Button>
    );
  } else if (harAktivDeltakelse) {
    return (
      <Alert variant="success">
        <Heading level={"2"} size="small">
          Aktiv deltakelse
        </Heading>
        <VStack>
          <BodyShort spacing>Bruker deltar på tiltaket</BodyShort>
          {vedtakRoute ? (
            <BodyShort>
              <Link href={vedtakRoute.href}>Gå til vedtaket</Link>
            </BodyShort>
          ) : null}
        </VStack>
      </Alert>
    );
  }

  return null;
}
