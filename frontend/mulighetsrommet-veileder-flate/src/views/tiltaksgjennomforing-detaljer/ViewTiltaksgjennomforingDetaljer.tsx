import { Chat2Icon, CheckmarkIcon, PadlockLockedFillIcon } from "@navikt/aksel-icons";
import { Alert, Button } from "@navikt/ds-react";
import { useAtomValue } from "jotai";
import {
  Bruker,
  DelMedBruker,
  Innsatsgruppe,
  NavVeileder,
  Tiltakskode,
  Toggles,
  VeilederflateTiltaksgjennomforing,
} from "mulighetsrommet-api-client";
import { Link, Outlet } from "react-router-dom";
import { BrukerHarIkke14aVedtakVarsel } from "../../components/ikkeKvalifisertVarsel/BrukerHarIkke14aVedtakVarsel";
import { BrukerKvalifisererIkkeVarsel } from "../../components/ikkeKvalifisertVarsel/BrukerKvalifisererIkkeVarsel";
import { DetaljerJoyride } from "../../components/joyride/DetaljerJoyride";
import { OpprettAvtaleJoyride } from "../../components/joyride/OpprettAvtaleJoyride";
import { Delemodal } from "../../components/modal/delemodal/Delemodal";
import SidemenyDetaljer from "../../components/sidemeny/SidemenyDetaljer";
import TiltaksdetaljerFane from "../../components/tabs/TiltaksdetaljerFane";
import Tilbakeknapp from "../../components/tilbakeknapp/Tilbakeknapp";
import { useGetTiltaksgjennomforingIdFraUrl } from "../../core/api/queries/useGetTiltaksgjennomforingIdFraUrl";
import { paginationAtom } from "../../core/atoms/atoms";
import { environments } from "../../env";
import TiltaksgjennomforingsHeader from "../../layouts/TiltaksgjennomforingsHeader";
import { byttTilDialogFlate } from "../../utils/DialogFlateUtils";
import { erPreview, formaterDato } from "../../utils/Utils";
import styles from "./ViewTiltaksgjennomforingDetaljer.module.scss";
import { useDelMedBruker } from "../../components/modal/delemodal/DelemodalReducer";
import { useLogEvent } from "../../logging/amplitude";
import { utledDelMedBrukerTekst } from "../../components/modal/delemodal/DelMedBrukerTekst";
import { erBrukerResertMotElektroniskKommunikasjon } from "../../utils/Bruker";
import { useFeatureToggle } from "../../core/api/feature-toggles";

const whiteListOpprettAvtaleKnapp: Tiltakskode[] = [
  Tiltakskode.MIDLONTIL,
  Tiltakskode.ARBTREN,
  Tiltakskode.VARLONTIL,
  Tiltakskode.MENTOR,
  Tiltakskode.INKLUTILS,
  Tiltakskode.TILSJOBB,
];

type IndividuelleTiltak = (typeof whiteListOpprettAvtaleKnapp)[number];

function tiltakstypeAsStringIsIndividuellTiltakstype(
  arenakode: Tiltakskode,
): arenakode is IndividuelleTiltak {
  return whiteListOpprettAvtaleKnapp.includes(arenakode);
}

function lenkeTilOpprettAvtaleForEnv(): string {
  const env: environments = import.meta.env.VITE_ENVIRONMENT;
  const baseUrl =
    env === "production"
      ? "https://tiltaksgjennomforing.intern.nav.no/"
      : "https://tiltaksgjennomforing.intern.dev.nav.no/";
  return `${baseUrl}tiltaksgjennomforing/opprett-avtale`;
}

function resolveName(ansatt?: NavVeileder) {
  if (!ansatt) {
    return "";
  }

  return [ansatt.fornavn, ansatt.etternavn].filter((part) => part !== "").join(" ");
}

interface Props {
  tiltaksgjennomforing: VeilederflateTiltaksgjennomforing;
  brukerHarRettPaaTiltak: boolean;
  brukersInnsatsgruppe?: Innsatsgruppe;
  innsatsgruppeForGjennomforing: Innsatsgruppe;
  harDeltMedBruker?: DelMedBruker;
  veilederdata: NavVeileder;
  brukerdata: Bruker;
}

const ViewTiltaksgjennomforingDetaljer = ({
  tiltaksgjennomforing,
  harDeltMedBruker,
  brukerHarRettPaaTiltak,
  innsatsgruppeForGjennomforing,
  veilederdata,
  brukerdata,
}: Props) => {
  const gjennomforingsId = useGetTiltaksgjennomforingIdFraUrl();
  const pageData = useAtomValue(paginationAtom);
  const veiledernavn = resolveName(veilederdata);
  const datoSidenSistDelt =
    harDeltMedBruker && formaterDato(new Date(harDeltMedBruker.createdAt!!));
  const { logEvent } = useLogEvent();
  const originaldeletekstFraTiltakstypen = tiltaksgjennomforing.tiltakstype.delingMedBruker ?? "";
  const brukernavn = erPreview() ? "{Navn}" : brukerdata?.fornavn;
  const { data: enableDeltakerRegistrering } = useFeatureToggle(
    Toggles.MULIGHETSROMMET_VEILEDERFLATE_VIS_DELTAKER_REGISTRERING,
  );

  const deletekst = utledDelMedBrukerTekst(
    originaldeletekstFraTiltakstypen,
    tiltaksgjennomforing.navn,
    brukernavn,
  );
  const [state, dispatch] = useDelMedBruker(deletekst);

  const handleClickApneModal = () => {
    const { reservert } = erBrukerResertMotElektroniskKommunikasjon(brukerdata);
    logEvent({
      name: "arbeidsmarkedstiltak.del-med-bruker",
      data: { action: "Åpnet delemodal", tiltakstype: tiltaksgjennomforing.tiltakstype.navn },
    });
    reservert
      ? dispatch({ type: "Toggle statusmodal", payload: true })
      : dispatch({ type: "Toggle modal", payload: true });
  };

  if (!tiltaksgjennomforing) {
    return (
      <Alert variant="warning">{`Det finnes ingen tiltaksgjennomføringer med id: "${gjennomforingsId}"`}</Alert>
    );
  }

  const kanBrukerFaaAvtale = () => {
    if (
      tiltaksgjennomforing.tiltakstype?.arenakode &&
      tiltakstypeAsStringIsIndividuellTiltakstype(tiltaksgjennomforing.tiltakstype.arenakode)
    ) {
      const url = lenkeTilOpprettAvtaleForEnv();
      window.open(url, "_blank");
    }
  };

  const opprettAvtale =
    !!tiltaksgjennomforing.tiltakstype?.arenakode &&
    tiltakstypeAsStringIsIndividuellTiltakstype(tiltaksgjennomforing.tiltakstype.arenakode) &&
    !erPreview();

  return (
    <div className={styles.container}>
      <div className={styles.top_wrapper}>
        {!erPreview() && (
          <Tilbakeknapp
            tilbakelenke={`/arbeidsmarkedstiltak/oversikt#pagination=${encodeURIComponent(
              JSON.stringify({ ...pageData }),
            )}`}
            tekst="Tilbake til tiltaksoversikten"
          />
        )}
        {!erPreview() && (
          <div>
            <DetaljerJoyride opprettAvtale={opprettAvtale} />
            {opprettAvtale ? <OpprettAvtaleJoyride opprettAvtale={opprettAvtale} /> : null}
          </div>
        )}
      </div>
      <BrukerKvalifisererIkkeVarsel
        brukerdata={brukerdata}
        brukerHarRettPaaTiltak={brukerHarRettPaaTiltak}
        innsatsgruppeForGjennomforing={innsatsgruppeForGjennomforing}
      />
      <BrukerHarIkke14aVedtakVarsel brukerdata={brukerdata} />
      <div className={styles.tiltaksgjennomforing_detaljer} id="tiltaksgjennomforing_detaljer">
        <div className={styles.tiltakstype_header_maksbredde}>
          <TiltaksgjennomforingsHeader tiltaksgjennomforing={tiltaksgjennomforing} />
        </div>
        {!tiltaksgjennomforing.apentForInnsok && (
          <div className={styles.apent_for_innsok_status}>
            <PadlockLockedFillIcon title="Tiltaket er stengt for innsøking" />
          </div>
        )}
        <div className={styles.sidemeny}>
          <SidemenyDetaljer tiltaksgjennomforing={tiltaksgjennomforing} />
          <div className={styles.deleknapp_container}>
            {opprettAvtale && (
              <Button
                onClick={kanBrukerFaaAvtale}
                variant="primary"
                className={styles.deleknapp}
                aria-label="Opprett avtale"
                data-testid="opprettavtaleknapp"
                disabled={!brukerHarRettPaaTiltak}
              >
                Opprett avtale
              </Button>
            )}
            {enableDeltakerRegistrering && !opprettAvtale ? (
              <Link className={styles.link} to="./deltaker">
                Meld på
              </Link>
            ) : null}
            <Button
              onClick={handleClickApneModal}
              variant="secondary"
              className={styles.deleknapp}
              aria-label="Dele"
              data-testid="deleknapp"
              icon={harDeltMedBruker && <CheckmarkIcon title="Suksess" />}
              iconPosition="left"
            >
              {harDeltMedBruker && !erPreview()
                ? `Delt med bruker ${datoSidenSistDelt}`
                : "Del med bruker"}
            </Button>
          </div>
          {!brukerdata?.manuellStatus && !erPreview() && (
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
          {harDeltMedBruker && !erPreview() && (
            <div className={styles.dialogknapp}>
              <Button
                size="small"
                variant="tertiary"
                onClick={(event) =>
                  byttTilDialogFlate({
                    event,
                    dialogId: harDeltMedBruker.dialogId!!,
                  })
                }
              >
                Åpne i dialogen
                <Chat2Icon aria-label="Åpne i dialogen" />
              </Button>
            </div>
          )}
        </div>
        <TiltaksdetaljerFane tiltaksgjennomforing={tiltaksgjennomforing} />
        <Delemodal
          brukernavn={erPreview() ? "{Navn}" : brukerdata?.fornavn}
          veiledernavn={erPreview() ? "{Veiledernavn}" : veiledernavn}
          brukerFnr={brukerdata.fnr}
          tiltaksgjennomforing={tiltaksgjennomforing}
          brukerdata={brukerdata}
          harDeltMedBruker={harDeltMedBruker}
          dispatch={dispatch}
          state={state}
        />
      </div>
      <div className={styles.oppskriftContainer}>
        <Outlet />
      </div>
    </div>
  );
};

export default ViewTiltaksgjennomforingDetaljer;
