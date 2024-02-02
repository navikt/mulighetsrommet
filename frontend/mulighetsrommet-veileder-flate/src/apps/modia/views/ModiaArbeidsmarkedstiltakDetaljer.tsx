import { Link } from "react-router-dom";
import { useAtomValue } from "jotai";
import { Chat2Icon } from "@navikt/aksel-icons";
import { Alert, Button } from "@navikt/ds-react";
import {
  Bruker,
  Innsatsgruppe,
  NavVeileder,
  Tiltakskode,
  Toggles,
  VeilederflateTiltakstype,
} from "mulighetsrommet-api-client";
import { useTitle } from "mulighetsrommet-frontend-common";
import { useHentBrukerdata } from "@/apps/modia/hooks/useHentBrukerdata";
import { useHentDeltMedBrukerStatus } from "@/apps/modia/hooks/useHentDeltMedbrukerStatus";
import { useHentVeilederdata } from "@/apps/modia/hooks/useHentVeilederdata";
import { useTiltaksgjennomforingById } from "@/core/api/queries/useTiltaksgjennomforingById";
import { useModiaContext } from "@/apps/modia/hooks/useModiaContext";
import { ViewTiltaksgjennomforingDetaljer } from "@/layouts/ViewTiltaksgjennomforingDetaljer";
import styles from "./ModiaArbeidsmarkedstiltakDetaljer.module.scss";
import { Tilbakeknapp } from "@/components/tilbakeknapp/Tilbakeknapp";
import { DetaljerJoyride } from "@/components/joyride/DetaljerJoyride";
import { OpprettAvtaleJoyride } from "@/components/joyride/OpprettAvtaleJoyride";
import { BrukerKvalifisererIkkeVarsel } from "@/components/varsler/BrukerKvalifisererIkkeVarsel";
import { BrukerHarIkke14aVedtakVarsel } from "@/components/varsler/BrukerHarIkke14aVedtakVarsel";
import { byttTilDialogFlate } from "@/utils/DialogFlateUtils";
import { paginationAtom } from "@/core/atoms/atoms";
import { useFeatureToggle } from "@/core/api/feature-toggles";
import { DelMedBruker } from "@/components/delMedBruker/DelMedBruker";
import { TiltakLoader } from "@/components/TiltakLoader";
import { useGetTiltaksgjennomforingIdFraUrl } from "@/core/api/queries/useGetTiltaksgjennomforingIdFraUrl";
import { isProduction } from "@/environment";

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

  return (
    <>
      <BrukerKvalifisererIkkeVarsel
        brukerdata={brukerdata}
        brukerHarRettPaaTiltak={brukerHarRettPaaValgtTiltak}
        tiltakstype={tiltakstype}
      />
      <BrukerHarIkke14aVedtakVarsel brukerdata={brukerdata} />
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
          <div className={styles.brukeractions_container}>
            {kanOppretteAvtaleForTiltak && (
              <Button
                onClick={() => {
                  const url = lenkeTilOpprettAvtale();
                  window.open(url, "_blank");
                }}
                variant="primary"
                className={styles.deleknapp}
                aria-label="Opprett avtale"
                data-testid="opprettavtaleknapp"
                disabled={!brukerHarRettPaaValgtTiltak}
              >
                Opprett avtale
              </Button>
            )}
            {enableDeltakerRegistrering && !kanOppretteAvtaleForTiltak ? (
              <Link className={styles.link} to="./deltaker">
                Meld på
              </Link>
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
            {delMedBrukerInfo && (
              <div className={styles.dialogknapp}>
                <Button
                  size="small"
                  variant="tertiary"
                  onClick={(event) =>
                    byttTilDialogFlate({
                      event,
                      dialogId: delMedBrukerInfo.dialogId,
                    })
                  }
                >
                  Åpne i dialogen
                  <Chat2Icon aria-label="Åpne i dialogen" />
                </Button>
              </div>
            )}
          </div>
        }
      />
    </>
  );
}

const whiteListOpprettAvtaleKnapp: Tiltakskode[] = [
  Tiltakskode.MIDLONTIL,
  Tiltakskode.ARBTREN,
  Tiltakskode.VARLONTIL,
  Tiltakskode.MENTOR,
  Tiltakskode.INKLUTILS,
  Tiltakskode.TILSJOBB,
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
