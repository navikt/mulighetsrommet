import { useRegioner } from "@/api/queries/useRegioner";
import { DelMedBruker } from "@/apps/modia/delMedBruker/DelMedBruker";
import { useHentBrukerdata } from "@/apps/modia/hooks/useHentBrukerdata";
import { useHentDeltMedBrukerStatus } from "@/apps/modia/hooks/useHentDeltMedbrukerStatus";
import { useHentVeilederdata } from "@/apps/modia/hooks/useHentVeilederdata";
import { useModiaContext } from "@/apps/modia/hooks/useModiaContext";
import { BrukerKvalifisererIkkeVarsel } from "@/apps/modia/varsler/BrukerKvalifisererIkkeVarsel";
import { DetaljerJoyride } from "@/components/joyride/DetaljerJoyride";
import { OpprettAvtaleJoyride } from "@/components/joyride/OpprettAvtaleJoyride";
import { PersonvernContainer } from "@/components/personvern/PersonvernContainer";
import { LenkeListe } from "@/components/sidemeny/Lenker";
import { Tilbakeknapp } from "@/components/tilbakeknapp/Tilbakeknapp";
import { PORTEN_URL_FOR_TILBAKEMELDING } from "@/constants";
import { paginationAtom } from "@/core/atoms";
import { isProduction } from "@/environment";
import { ViewTiltaksgjennomforingDetaljer } from "@/layouts/ViewTiltaksgjennomforingDetaljer";
import { Chat2Icon } from "@navikt/aksel-icons";
import { Alert, Button } from "@navikt/ds-react";
import { useAtomValue } from "jotai";
import {
  Bruker,
  NavVeileder,
  TiltakskodeArena,
  Toggles,
  VeilederflateTiltakstype,
} from "@mr/api-client";
import {
  DetaljerSkeleton,
  InlineErrorBoundary,
  TilbakemeldingLenke,
  useTitle,
} from "@mr/frontend-common";
import { gjennomforingIsAktiv } from "@mr/frontend-common/utils/utils";
import { useFeatureToggle } from "../../../api/feature-toggles";
import { useTiltaksgjennomforingById } from "../../../api/queries/useTiltaksgjennomforingById";
import { PameldingForGruppetiltak } from "../../../components/pamelding/PameldingForGruppetiltak";
import { VisibleWhenToggledOn } from "../../../components/toggles/VisibleWhenToggledOn";
import { useGetTiltaksgjennomforingIdFraUrl } from "../../../hooks/useGetTiltaksgjennomforingIdFraUrl";
import { ModiaRoute, resolveModiaRoute } from "../ModiaRoute";

export function ModiaArbeidsmarkedstiltakDetaljer() {
  const { fnr } = useModiaContext();
  const id = useGetTiltaksgjennomforingIdFraUrl();
  const { delMedBrukerInfo } = useHentDeltMedBrukerStatus(fnr, id);
  const { data: enableDeltakerRegistrering } = useFeatureToggle(
    Toggles.MULIGHETSROMMET_VEILEDERFLATE_VIS_DELTAKER_REGISTRERING,
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
  const regioner = useRegioner();

  useTitle(
    `Arbeidsmarkedstiltak - Detaljer ${
      tiltaksgjennomforing?.navn ? `- ${tiltaksgjennomforing.navn}` : null
    }`,
  );

  const pagination = useAtomValue(paginationAtom);

  if (isPendingTiltak || isPendingVeilederdata || isPendingBrukerdata) {
    return <DetaljerSkeleton />;
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

            {enableDeltakerRegistrering &&
            gjennomforingIsAktiv(tiltaksgjennomforing.status.status) ? (
              <PameldingForGruppetiltak
                kanOppretteAvtaleForTiltak={kanOppretteAvtaleForTiltak}
                brukerHarRettPaaValgtTiltak={brukerHarRettPaaValgtTiltak}
                tiltaksgjennomforing={tiltaksgjennomforing}
              />
            ) : null}

            {brukerdata.erUnderOppfolging &&
            gjennomforingIsAktiv(tiltaksgjennomforing.status.status) ? (
              <DelMedBruker
                delMedBrukerInfo={delMedBrukerInfo ?? undefined}
                veiledernavn={resolveName(veilederdata)}
                tiltaksgjennomforing={tiltaksgjennomforing}
                bruker={brukerdata}
              />
            ) : null}

            {!brukerdata?.manuellStatus && (
              <Alert
                title="Vi kunne ikke opprette kontakt med KRR og vet derfor ikke om brukeren har reservert seg mot digital kommunikasjon"
                key="alert-innsatsgruppe"
                data-testid="alert-innsatsgruppe"
                size="small"
                variant="error"
              >
                Vi kunne ikke opprette kontakt med KRR og vet derfor ikke om brukeren har reservert
                seg mot digital kommunikasjon
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
            <VisibleWhenToggledOn toggle={Toggles.MULIGHETSROMMET_VEILEDERFLATE_VIS_TILBAKEMELDING}>
              <TilbakemeldingLenke
                url={PORTEN_URL_FOR_TILBAKEMELDING(
                  tiltaksgjennomforing.tiltaksnummer,
                  regioner?.data?.find((r) => r.enhetsnummer === tiltaksgjennomforing?.fylke)?.navn,
                )}
                tekst="Gi tilbakemelding via Porten"
              />
            </VisibleWhenToggledOn>
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
