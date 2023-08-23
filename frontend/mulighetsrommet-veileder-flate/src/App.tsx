import { initializeFaro, WebVitalsInstrumentation } from '@grafana/faro-web-sdk';
import { Toggles } from 'mulighetsrommet-api-client';
import { ErrorBoundary } from 'react-error-boundary';
import { QueryClient, QueryClientProvider } from 'react-query';
import { ReactQueryDevtools } from 'react-query/devtools';
import { BrowserRouter as Router, Route, Routes } from 'react-router-dom';
import styles from './App.module.scss';
import RoutesConfig from './RoutesConfig';
import FakeDoor from './components/fakedoor/FakeDoor';
import { APPLICATION_NAME } from './constants';
import { useFeatureToggle } from './core/api/feature-toggles';
import { useHentVeilederdata } from './core/api/queries/useHentVeilederdata';
import { FnrContext } from './hooks/useFnr';
import { useInitialBrukerfilter } from './hooks/useInitialBrukerfilter';
import { ErrorFallback } from './utils/ErrorFallback';
import { SanityPreview } from './views/Preview/SanityPreview';
import React from 'react';

if (import.meta.env.PROD) {
  initializeFaro({
    url: import.meta.env.VITE_FARO_URL || 'http://localhost:3000/collect',
    instrumentations: [new WebVitalsInstrumentation()],
    app: {
      name: 'mulighetsrommet-veileder-flate',
    },
  });
}

const queryClient = new QueryClient({
  defaultOptions: {
    queries: {
      refetchOnWindowFocus: import.meta.env.PROD,
      retry: import.meta.env.PROD,
    },
  },
});

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

export interface AppProps {
  fnr: string;
}

function App(props: AppProps) {
  return (
    <ErrorBoundary FallbackComponent={ErrorFallback}>
      <div className={styles.app_container}>
        <div className={APPLICATION_NAME}>
          <QueryClientProvider client={queryClient}>
            <FnrContext.Provider value={props.fnr}>
              <Router>
                <Routes>
                  <Route path="preview/:tiltaksnummer" element={<SanityPreview />} />
                  <Route path="*" element={<AppWrapper />} />
                </Routes>
              </Router>
            </FnrContext.Provider>
            <ReactQueryDevtools initialIsOpen={false} />
          </QueryClientProvider>
        </div>
      </div>
    </ErrorBoundary>
  );
}

export default App;
