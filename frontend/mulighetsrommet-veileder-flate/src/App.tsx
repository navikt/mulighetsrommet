import { Modal } from '@navikt/ds-react';
import { ErrorBoundary } from 'react-error-boundary';
import { QueryClient, QueryClientProvider } from 'react-query';
import { ReactQueryDevtools } from 'react-query/devtools';
import { BrowserRouter as Router, Navigate, Route, Routes } from 'react-router-dom';
import FakeDoor from './components/fakedoor/FakeDoor';
import { APPLICATION_NAME, MODAL_ACCESSIBILITY_WRAPPER } from './constants';
import { ENABLE_ARBEIDSFLATE, useFeatureToggles } from './core/api/feature-toggles';
import { useHentVeilederdata } from './core/api/queries/useHentVeilederdata';
import { useHentFnrFraUrl } from './hooks/useHentFnrFraUrl';
import { useInitialBrukerfilter } from './hooks/useInitialBrukerfilter';
import RoutesConfig from './RoutesConfig';
import { ErrorFallback } from './utils/ErrorFallback';
import styles from './App.module.scss';
import { SanityPreview } from './views/Preview/SanityPreview';
import { WebVitalsInstrumentation, initializeFaro } from '@grafana/faro-web-sdk';

if (import.meta.env.PROD) {
  initializeFaro({
    url: import.meta.env.VITE_FARO_URL || 'http://localhost:3000/collect',
    instrumentations: [new WebVitalsInstrumentation()],
    app: {
      name: 'mulighetsrommet-veileder-flate',
    },
  });
}
// Trengs for at tab og fokus ikke skal gå utenfor modal når den er åpen.
Modal.setAppElement?.(`#${MODAL_ACCESSIBILITY_WRAPPER}`);

const queryClient = new QueryClient({
  defaultOptions: {
    queries: {
      refetchOnWindowFocus: process.env.NODE_ENV !== 'development',
      retry: process.env.NODE_ENV !== 'development',
    },
  },
});

function AppWrapper() {
  const features = useFeatureToggles();
  useInitialBrukerfilter();
  useHentVeilederdata(); // Pre-fetch veilederdata så slipper vi å vente på data når vi trenger det i appen senere

  const enableArbeidsflate = features.isSuccess && features.data[ENABLE_ARBEIDSFLATE];

  if (features.isLoading) {
    // Passer på at vi ikke flash-viser løsningen før vi har hentet toggle for fake-door
    return null;
  }

  if (!enableArbeidsflate) return <FakeDoor />;

  return <RoutesConfig />;
}

function App() {
  const fnr = useHentFnrFraUrl();

  return (
    <ErrorBoundary FallbackComponent={ErrorFallback}>
      <div className={styles.app_container}>
        <div className={APPLICATION_NAME}>
          <QueryClientProvider client={queryClient}>
            <Router>
              <Routes>
                <Route path=":fnr/*" element={<AppWrapper />} />
                <Route path="preview/:tiltaksnummer" element={<SanityPreview />} />
                <Route path="*" element={<Navigate to={`${fnr}`} />}>
                  {/* Fallback-rute dersom ingenting matcher. Returner bruker til startside */}
                </Route>
              </Routes>
            </Router>
            <ReactQueryDevtools initialIsOpen={false} />
          </QueryClientProvider>
        </div>
      </div>
    </ErrorBoundary>
  );
}

export default App;
