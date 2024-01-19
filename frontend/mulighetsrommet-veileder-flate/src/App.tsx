import { getWebInstrumentations, initializeFaro } from "@grafana/faro-web-sdk";
import "@navikt/ds-css";
import { ErrorBoundary } from "react-error-boundary";
import { BrowserRouter as Router, Navigate, Route, Routes } from "react-router-dom";
import styles from "./App.module.scss";
import { Oppskrift } from "./components/oppskrift/Oppskrift";
import { APPLICATION_NAME } from "./constants";
import { useHentVeilederdata } from "./core/api/queries/useHentVeilederdata";
import { useInitializeArbeidsmarkedstiltakFilterForBruker } from "./hooks/useInitializeArbeidsmarkedstiltakFilterForBruker";
import { useInitializeAppContext } from "./hooks/useInitializeAppContext";
import { initAmplitude, useLogEvent } from "./logging/amplitude";
import { ErrorFallback } from "./utils/ErrorFallback";
import { SanityPreview } from "./views/Preview/SanityPreview";
import { SanityPreviewOversikt } from "./views/Preview/SanityPreviewOversikt";
import { useFeatureToggle } from "./core/api/feature-toggles";
import { Toggles } from "mulighetsrommet-api-client";
import { Landingsside } from "./views/landingsside/Landingsside";
import { ViewTiltaksgjennomforingDetaljerContainer } from "./views/tiltaksgjennomforing-detaljer/ViewTiltaksgjennomforingDetaljerContainer";
import { DeltakerRegistrering } from "./microfrontends/team_komet/DeltakerRegistrering";
import ViewTiltaksgjennomforingOversikt from "./views/tiltaksgjennomforing-oversikt/ViewTiltaksgjennomforingOversikt";
import { useEffect } from "react";

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
      <div className={styles.app_container}>
        <div className={APPLICATION_NAME}>
          <Router>
            <Routes>
              <Route path="preview/*" element={<PreviewArbeidsmarkedstiltak />} />
              <Route path="arbeidsmarkedstiltak/*" element={<PersonflateArbeidsmarkedstiltak />} />
              <Route path="*" element={<Navigate replace to="/arbeidsmarkedstiltak" />} />
            </Routes>
          </Router>
        </div>
      </div>
    </ErrorBoundary>
  );
}

function PreviewArbeidsmarkedstiltak() {
  return (
    <Routes>
      <Route path="oversikt" element={<SanityPreviewOversikt />} />
      <Route path="tiltak/:id" element={<SanityPreview />}>
        <Route path="oppskrifter/:oppskriftId/:tiltakstypeId" element={<Oppskrift />} />
      </Route>
      <Route path="*" element={<Navigate replace to="/preview/oversikt" />} />
    </Routes>
  );
}

function PersonflateArbeidsmarkedstiltak() {
  useHentVeilederdata(); // Pre-fetch veilederdata så slipper vi å vente på data når vi trenger det i appen senere

  const { logEvent } = useLogEvent();
  useEffect(() => {
    logEvent({ name: "arbeidsmarkedstiltak.unike-brukere" });
  }, []);

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
    <Routes>
      {enableLandingsside ? <Route path="" element={<Landingsside />} /> : null}
      <Route path="oversikt" element={<ViewTiltaksgjennomforingOversikt />} />
      <Route path="tiltak/:id" element={<ViewTiltaksgjennomforingDetaljerContainer />}>
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
  );
}
