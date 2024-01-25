import { getWebInstrumentations, initializeFaro } from "@grafana/faro-web-sdk";
import "@navikt/ds-css";
import { ErrorBoundary } from "react-error-boundary";
import { BrowserRouter as Router, Navigate, Route, Routes } from "react-router-dom";
import styles from "./App.module.scss";
import { Oppskrift } from "./components/oppskrift/Oppskrift";
import { useHentVeilederdata } from "./core/api/queries/useHentVeilederdata";
import { useInitializeArbeidsmarkedstiltakFilterForBruker } from "./hooks/useInitializeArbeidsmarkedstiltakFilterForBruker";
import { useInitializeAppContext } from "./hooks/useInitializeAppContext";
import { initAmplitude } from "./logging/amplitude";
import { ErrorFallback } from "./utils/ErrorFallback";
import { useFeatureToggle } from "./core/api/feature-toggles";
import { Toggles } from "mulighetsrommet-api-client";
import { Landingsside } from "./views/landingsside/Landingsside";
import { ModiaTiltaksgjennomforingDetaljer } from "./views/modia-arbeidsmarkedstiltak/ModiaTiltaksgjennomforingDetaljer";
import { DeltakerRegistrering } from "./microfrontends/team_komet/DeltakerRegistrering";
import ModiaViewTiltaksgjennomforingOversikt from "./views/modia-arbeidsmarkedstiltak/ModiaViewTiltaksgjennomforingOversikt";
import { ArbeidsmarkedstiltakHeader } from "./components/ArbeidsmarkedstiltakHeader";
import { PreviewOversikt } from "./views/Preview/PreviewOversikt";
import { PreviewViewTiltaksgjennomforingDetaljer } from "./views/Preview/PreviewViewTiltaksgjennomforingDetaljer";

if (import.meta.env.PROD && import.meta.env.VITE_FARO_URL) {
  initializeFaro({
    url: import.meta.env.VITE_FARO_URL,
    instrumentations: [...getWebInstrumentations({ captureConsole: true })],
    app: {
      name: "mulighetsrommet-veileder-flate",
    },
  });
  initAmplitude();
}

export function App() {
  return (
    <ErrorBoundary FallbackComponent={ErrorFallback}>
      <Router>
        <Routes>
          <Route path="preview/*" element={<PreviewArbeidsmarkedstiltak />} />
          <Route path="arbeidsmarkedstiltak/*" element={<ModiaArbeidsmarkedstiltak />} />
          <Route path="*" element={<Navigate replace to="/arbeidsmarkedstiltak" />} />
        </Routes>
      </Router>
    </ErrorBoundary>
  );
}

function PreviewArbeidsmarkedstiltak() {
  return (
    <div className={styles.preview_container}>
      <ArbeidsmarkedstiltakHeader />
      <div className={styles.preview_content}>
        <Routes>
          <Route path="oversikt" element={<PreviewOversikt />} />
          <Route path="tiltak/:id" element={<PreviewViewTiltaksgjennomforingDetaljer />}>
            <Route path="oppskrifter/:oppskriftId/:tiltakstypeId" element={<Oppskrift />} />
          </Route>
          <Route path="*" element={<Navigate replace to="/preview/oversikt" />} />
        </Routes>
      </div>
    </div>
  );
}

function ModiaArbeidsmarkedstiltak() {
  useHentVeilederdata(); // Pre-fetch veilederdata så slipper vi å vente på data når vi trenger det i appen senere

  const { fnr, enhet } = useInitializeAppContext();

  useInitializeArbeidsmarkedstiltakFilterForBruker();

  const enableLandingssideFeature = useFeatureToggle(
    Toggles.MULIGHETSROMMET_VEILEDERFLATE_LANDINGSSIDE,
  );
  const visDeltakerregistreringFeature = useFeatureToggle(
    Toggles.MULIGHETSROMMET_VEILEDERFLATE_VIS_DELTAKER_REGISTRERING,
  );
  const enableLandingsside = enableLandingssideFeature.isSuccess && enableLandingssideFeature.data;
  const visDeltakerregistrering =
    visDeltakerregistreringFeature.isSuccess && visDeltakerregistreringFeature.data;

  if (enableLandingssideFeature.isLoading) {
    return null;
  }

  return (
    <div className={styles.amt_container}>
      <div className={styles.amt_content}>
        <Routes>
          {enableLandingsside ? <Route path="" element={<Landingsside />} /> : null}
          <Route path="oversikt" element={<ModiaViewTiltaksgjennomforingOversikt />} />
          <Route path="tiltak/:id" element={<ModiaTiltaksgjennomforingDetaljer />}>
            <Route path="oppskrifter/:oppskriftId/:tiltakstypeId" element={<Oppskrift />} />
          </Route>
          {visDeltakerregistrering ? (
            <Route
              path="tiltak/:id/deltaker"
              element={<DeltakerRegistrering fnr={fnr} enhetId={enhet} />}
            />
          ) : null}
          <Route
            path="*"
            element={
              <Navigate
                replace
                to={enableLandingsside ? "/arbeidsmarkedstiltak" : "/arbeidsmarkedstiltak/oversikt"}
              />
            }
          />
        </Routes>
      </div>
    </div>
  );
}
