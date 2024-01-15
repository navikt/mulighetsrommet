import { getWebInstrumentations, initializeFaro } from "@grafana/faro-web-sdk";
import "@navikt/ds-css";
import { Toggles } from "mulighetsrommet-api-client";
import { ErrorBoundary } from "react-error-boundary";
import { Route, BrowserRouter as Router, Routes } from "react-router-dom";
import styles from "./App.module.scss";
import FakeDoor from "./components/fakedoor/FakeDoor";
import { Oppskrift } from "./components/oppskrift/Oppskrift";
import { APPLICATION_NAME } from "./constants";
import { useFeatureToggle } from "./core/api/feature-toggles";
import { useHentVeilederdata } from "./core/api/queries/useHentVeilederdata";
import { useInitialBrukerfilter } from "./hooks/useInitialBrukerfilter";
import { useUpdateAppContext } from "./hooks/useUpdateAppContext";
import { initAmplitude } from "./logging/amplitude";
import RoutesConfig from "./RoutesConfig";
import { ErrorFallback } from "./utils/ErrorFallback";
import { SanityPreview } from "./views/Preview/SanityPreview";
import { SanityPreviewOversikt } from "./views/Preview/SanityPreviewOversikt";

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

function AppInnhold() {
  useInitialBrukerfilter();
  useHentVeilederdata(); // Pre-fetch veilederdata så slipper vi å vente på data når vi trenger det i appen senere
  useUpdateAppContext();

  return <RoutesConfig />;
}

export function App() {
  return (
    <ErrorBoundary FallbackComponent={ErrorFallback}>
      <div className={styles.app_container}>
        <div className={APPLICATION_NAME}>
          <Router>
            <Routes>
              <Route path="preview" element={<SanityPreviewOversikt />} />
              <Route path="preview/:id" element={<SanityPreview />}>
                <Route path="oppskrifter/:oppskriftId/:tiltakstypeId" element={<Oppskrift />} />
              </Route>
              <Route path="*" element={<AppInnhold />} />
            </Routes>
          </Router>
        </div>
      </div>
    </ErrorBoundary>
  );
}
