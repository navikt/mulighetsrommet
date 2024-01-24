import { Alert, Button, Loader } from "@navikt/ds-react";
import { useTitle } from "mulighetsrommet-frontend-common";
import { useHentBrukerdata } from "../../core/api/queries/useHentBrukerdata";
import { useHentDeltMedBrukerStatus } from "../../core/api/queries/useHentDeltMedbrukerStatus";
import { useHentVeilederdata } from "../../core/api/queries/useHentVeilederdata";
import useTiltaksgjennomforingById from "../../core/api/queries/useTiltaksgjennomforingById";
import { useBrukerHarRettPaaTiltak } from "../../hooks/useBrukerHarRettPaaTiltak";
import { useAppContext } from "../../hooks/useAppContext";
import ViewTiltaksgjennomforingDetaljer from "./ViewTiltaksgjennomforingDetaljer";
import styles from "./ViewTiltaksgjennomforingDetaljer.module.scss";
import Tilbakeknapp from "../../components/tilbakeknapp/Tilbakeknapp";
import { DetaljerJoyride } from "../../components/joyride/DetaljerJoyride";
import { OpprettAvtaleJoyride } from "../../components/joyride/OpprettAvtaleJoyride";
import { BrukerKvalifisererIkkeVarsel } from "../../components/ikkeKvalifisertVarsel/BrukerKvalifisererIkkeVarsel";
import { BrukerHarIkke14aVedtakVarsel } from "../../components/ikkeKvalifisertVarsel/BrukerHarIkke14aVedtakVarsel";
import { Link } from "react-router-dom";
import { Chat2Icon } from "@navikt/aksel-icons";
import { byttTilDialogFlate } from "../../utils/DialogFlateUtils";
import { useAtomValue } from "jotai/index";
import { paginationAtom } from "../../core/atoms/atoms";
import { useFeatureToggle } from "../../core/api/feature-toggles";
import { NavVeileder, Tiltakskode, Toggles } from "mulighetsrommet-api-client";
import { environments } from "../../env";
import { DelMedBruker } from "../../components/delMedBruker/DelMedBruker";
import { formaterDato } from "../../utils/Utils";

const whiteListOpprettAvtaleKnapp: Tiltakskode[] = [
  Tiltakskode.MIDLONTIL,
  Tiltakskode.ARBTREN,
  Tiltakskode.VARLONTIL,
  Tiltakskode.MENTOR,
  Tiltakskode.INKLUTILS,
  Tiltakskode.TILSJOBB,
];

type IndividuelleTiltak = (typeof whiteListOpprettAvtaleKnapp)[number];

function resolveName(ansatt?: NavVeileder) {
  if (!ansatt) {
    return "";
  }
  return [ansatt.fornavn, ansatt.etternavn].filter((part) => part !== "").join(" ");
}

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

export function ModiaTiltaksgjennomforingDetaljer() {
  const { data: tiltaksgjennomforing, isLoading, isError } = useTiltaksgjennomforingById();
  useTitle(
    `Arbeidsmarkedstiltak - Detaljer ${
      tiltaksgjennomforing?.navn ? `- ${tiltaksgjennomforing.navn}` : null
    }`,
  );
  const { fnr } = useAppContext();
  const { harDeltMedBruker } = useHentDeltMedBrukerStatus(fnr, tiltaksgjennomforing);
  const { brukerHarRettPaaTiltak, innsatsgruppeForGjennomforing } = useBrukerHarRettPaaTiltak();
  const veilederdata = useHentVeilederdata();
  const brukerdata = useHentBrukerdata().data;
  const pageData = useAtomValue(paginationAtom);

  const { data: enableDeltakerRegistrering } = useFeatureToggle(
    Toggles.MULIGHETSROMMET_VEILEDERFLATE_VIS_DELTAKER_REGISTRERING,
  );

  if (isLoading) {
    return (
      <div className={styles.filter_loader}>
        <Loader size="xlarge" />
      </div>
    );
  }

  if (isError) {
    return <Alert variant="error">Det har skjedd en feil</Alert>;
  }

  if (!tiltaksgjennomforing || !veilederdata?.data || !brukerdata) return null;

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
    tiltakstypeAsStringIsIndividuellTiltakstype(tiltaksgjennomforing.tiltakstype.arenakode);

  const datoSidenSistDelt =
    harDeltMedBruker && formaterDato(new Date(harDeltMedBruker.createdAt!!));

  return (
    <>
      <BrukerKvalifisererIkkeVarsel
        brukerdata={brukerdata}
        brukerHarRettPaaTiltak={brukerHarRettPaaTiltak}
        innsatsgruppeForGjennomforing={innsatsgruppeForGjennomforing}
      />
      <BrukerHarIkke14aVedtakVarsel brukerdata={brukerdata} />
      <ViewTiltaksgjennomforingDetaljer
        tiltaksgjennomforing={tiltaksgjennomforing}
        knapperad={
          <>
            <Tilbakeknapp
              tilbakelenke={`/arbeidsmarkedstiltak/oversikt#pagination=${encodeURIComponent(
                JSON.stringify({ ...pageData }),
              )}`}
              tekst="Tilbake til tiltaksoversikten"
            />
            <div>
              <DetaljerJoyride opprettAvtale={opprettAvtale} />
              {opprettAvtale ? <OpprettAvtaleJoyride opprettAvtale={opprettAvtale} /> : null}
            </div>
          </>
        }
        brukerActions={
          <>
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
              <DelMedBruker
                knappetekst={
                  harDeltMedBruker ? `Delt med bruker ${datoSidenSistDelt}` : "Del med bruker"
                }
                veiledernavn={resolveName(veilederdata.data)}
                brukerdata={brukerdata}
                tiltaksgjennomforing={tiltaksgjennomforing}
              />
            </div>
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
            {harDeltMedBruker && (
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
          </>
        }
      />
    </>
  );
}
