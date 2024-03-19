import { DelMedBruker } from "@/apps/modia/delMedBruker/DelMedBruker";
import { useHentBrukerdata } from "@/apps/modia/hooks/useHentBrukerdata";
import { useHentDeltMedBrukerStatus } from "@/apps/modia/hooks/useHentDeltMedbrukerStatus";
import { useHentVeilederdata } from "@/apps/modia/hooks/useHentVeilederdata";
import { useModiaContext } from "@/apps/modia/hooks/useModiaContext";
import { BrukerKvalifisererIkkeVarsel } from "@/apps/modia/varsler/BrukerKvalifisererIkkeVarsel";
import { TiltakLoader } from "@/components/TiltakLoader";
import { DetaljerJoyride } from "@/components/joyride/DetaljerJoyride";
import { OpprettAvtaleJoyride } from "@/components/joyride/OpprettAvtaleJoyride";
import { Tilbakeknapp } from "@/components/tilbakeknapp/Tilbakeknapp";
import { useFeatureToggle } from "@/core/api/feature-toggles";
import { useGetTiltaksgjennomforingIdFraUrl } from "@/core/api/queries/useGetTiltaksgjennomforingIdFraUrl";
import { useTiltaksgjennomforingById } from "@/core/api/queries/useTiltaksgjennomforingById";
import { paginationAtom } from "@/core/atoms/atoms";
import { isProduction } from "@/environment";
import { ViewTiltaksgjennomforingDetaljer } from "@/layouts/ViewTiltaksgjennomforingDetaljer";
import { Chat2Icon } from "@navikt/aksel-icons";
import { Alert, Button } from "@navikt/ds-react";
import { useAtomValue } from "jotai";
import {
  Bruker,
  Innsatsgruppe,
  NavVeileder,
  TiltakskodeArena,
  Toggles,
  VeilederflateTiltakstype,
} from "mulighetsrommet-api-client";
import { useTitle } from "mulighetsrommet-frontend-common";
import styles from "./ModiaArbeidsmarkedstiltakDetaljer.module.scss";
import { LenkeListe } from "@/components/sidemeny/Lenker";
import { ModiaRoute, resolveModiaRoute } from "@/apps/modia/ModiaRoute";
import { Lenkeknapp } from "mulighetsrommet-frontend-common/components/lenkeknapp/Lenkeknapp";

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

  const { data: enableDeltakerRegistrering } = useFeatureToggle(
    Toggles.MULIGHETSROMMET_VEILEDERFLATE_VIS_DELTAKER_REGISTRERING,
  );

  if (isPendingTiltak || isPendingVeilederdata || isPendingBrukerdata) {
    return <TiltakLoader />;
  }

  if (isError || isErrorVeilederdata || isErrorBrukerdata) {
    return <Alert variant="error">Det har skjedd en feil</Alert>;
  }

  const tiltakstype = tiltaksgjennomforing.tiltakstype;
  const kanOppretteAvtaleForTiltak = isIndividueltTiltak(tiltakstype);
  const brukerHarRettPaaValgtTiltak = harBrukerRettPaaValgtTiltak(brukerdata, tiltakstype);
  const skalVisePameldingslenke =
    !kanOppretteAvtaleForTiltak &&
    brukerHarRettPaaValgtTiltak &&
    tiltakstypeStotterPamelding(tiltakstype);

  const opprettDeltakelseRoute = resolveModiaRoute({
    route: ModiaRoute.ARBEIDSMARKEDSTILTAK_OPPRETT_DELTAKELSE,
    gjennomforingId: id,
  });

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
        tiltakstype={tiltakstype}
      />
      <ViewTiltaksgjennomforingDetaljer
        tiltaksgjennomforing={tiltaksgjennomforing}
        knapperad={
          <>
            <Tilbakeknapp
              tilbakelenke={`/arbeidsmarkedstiltak/oversikt#pagination=${encodeURIComponent(
                JSON.stringify({ ...pagination }),
              )}`}
              tekst="Tilbake til tiltaksoversikten"
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

            {enableDeltakerRegistrering && skalVisePameldingslenke ? (
              <Lenkeknapp
                variant={"primary"}
                to={opprettDeltakelseRoute.href}
                onClick={opprettDeltakelseRoute.navigate}
              >
                Start påmelding
              </Lenkeknapp>
            ) : null}

            <DelMedBruker
              delMedBrukerInfo={delMedBrukerInfo}
              veiledernavn={resolveName(veilederdata)}
              tiltaksgjennomforing={tiltaksgjennomforing}
              brukerdata={brukerdata}
              lagreVeilederHarDeltTiltakMedBruker={lagreVeilederHarDeltTiltakMedBruker}
            />

            {!brukerdata?.manuellStatus && (
              <Alert
                title="Vi kunne ikke opprette kontakt med KRR og vet derfor ikke om brukeren har reservert seg mot elektronisk kommunikasjon"
                key="alert-innsatsgruppe"
                data-testid="alert-innsatsgruppe"
                size="small"
                variant="error"
                className={styles.alert}
              >
                Vi kunne ikke opprette kontakt med KRR og vet derfor ikke om brukeren har reservert
                seg mot elektronisk kommunikasjon
              </Alert>
            )}

            {dialogRoute && (
              <Lenkeknapp
                size="small"
                variant="tertiary"
                to={dialogRoute.href}
                onClick={dialogRoute.navigate}
              >
                Åpne i dialogen
                <Chat2Icon aria-label="Åpne i dialogen" />
              </Lenkeknapp>
            )}

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

function harBrukerRettPaaValgtTiltak(brukerdata: Bruker, tiltakstype: VeilederflateTiltakstype) {
  const innsatsgruppeForGjennomforing = tiltakstype.innsatsgruppe?.nokkel;

  if (!innsatsgruppeForGjennomforing) {
    return false;
  }

  const godkjenteInnsatsgrupper = brukerdata.innsatsgruppe
    ? utledInnsatsgrupperFraInnsatsgruppe(brukerdata.innsatsgruppe)
    : [];

  return godkjenteInnsatsgrupper.includes(innsatsgruppeForGjennomforing);
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

function utledInnsatsgrupperFraInnsatsgruppe(innsatsgruppe: string): Innsatsgruppe[] {
  switch (innsatsgruppe) {
    case "STANDARD_INNSATS":
      return [Innsatsgruppe.STANDARD_INNSATS];
    case "SITUASJONSBESTEMT_INNSATS":
      return [Innsatsgruppe.STANDARD_INNSATS, Innsatsgruppe.SITUASJONSBESTEMT_INNSATS];
    case "SPESIELT_TILPASSET_INNSATS":
      return [
        Innsatsgruppe.STANDARD_INNSATS,
        Innsatsgruppe.SITUASJONSBESTEMT_INNSATS,
        Innsatsgruppe.SPESIELT_TILPASSET_INNSATS,
      ];
    case "VARIG_TILPASSET_INNSATS":
      return [
        Innsatsgruppe.STANDARD_INNSATS,
        Innsatsgruppe.SITUASJONSBESTEMT_INNSATS,
        Innsatsgruppe.SPESIELT_TILPASSET_INNSATS,
        Innsatsgruppe.VARIG_TILPASSET_INNSATS,
      ];
    default:
      return [];
  }
}
