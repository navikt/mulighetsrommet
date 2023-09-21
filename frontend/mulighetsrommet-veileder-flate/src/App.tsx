import { initializeFaro, WebVitalsInstrumentation } from '@grafana/faro-web-sdk';
import '@navikt/ds-css';
import { Toggles } from 'mulighetsrommet-api-client';
import { ErrorBoundary } from 'react-error-boundary';
import { Navigate, Route, BrowserRouter as Router, Routes } from 'react-router-dom';
import styles from './App.module.scss';
import FakeDoor from './components/fakedoor/FakeDoor';
import { APPLICATION_NAME } from './constants';
import { useFeatureToggle } from './core/api/feature-toggles';
import { useHentVeilederdata } from './core/api/queries/useHentVeilederdata';
import { useInitialBrukerfilter } from './hooks/useInitialBrukerfilter';
import RoutesConfig from './RoutesConfig';
import { ErrorFallback } from './utils/ErrorFallback';
import { SanityPreview } from './views/Preview/SanityPreview';

if (import.meta.env.PROD && import.meta.env.VITE_FARO_URL) {
  initializeFaro({
    url: import.meta.env.VITE_FARO_URL,
    instrumentations: [new WebVitalsInstrumentation()],
    app: {
      name: 'mulighetsrommet-veileder-flate',
    },
  });
}

function AppWrapper() {
  useInitialBrukerfilter();
  useHentVeilederdata(); // Pre-fetch veilederdata så slipper vi å vente på data når vi trenger det i appen senere

  const feature = useFeatureToggle(Toggles.MULIGHETSROMMET_ENABLE_ARBEIDSFLATE);
  const enableArbeidsflate = feature.isSuccess && feature.data;

  if (feature.isLoading) {
    // Passer på at vi ikke flash-viser løsningen før vi har hentet toggle for fake-door
    return null;
  }

  if (!enableArbeidsflate) return <FakeDoor />;

  return <RoutesConfig />;
}

export function App() {
  return (
    <ErrorBoundary FallbackComponent={ErrorFallback}>
      <div className={styles.app_container}>
        <div className={APPLICATION_NAME}>
          <Router>
            <Routes>
              <Route path="preview/:tiltaksnummer" element={<SanityPreview />} />
              <Route path="*" element={<AppWrapper />} />
            </Routes>
          </Router>
        </div>
      </div>
    </ErrorBoundary>
  );
}
