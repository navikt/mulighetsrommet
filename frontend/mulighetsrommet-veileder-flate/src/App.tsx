import { Modal } from '@navikt/ds-react';
import { ErrorBoundary } from 'react-error-boundary';
import { QueryClient, QueryClientProvider } from 'react-query';
import { ReactQueryDevtools } from 'react-query/devtools';
import { BrowserRouter as Router, Navigate, Route, Routes } from 'react-router-dom';
import { ToastContainer } from 'react-toastify';
import 'react-toastify/dist/ReactToastify.css';
import './App.less';
import FakeDoor from './components/fakedoor/FakeDoor';
import Feedback from './components/feedback/Feedback';
import { APPLICATION_NAME, MODAL_ACCESSIBILITY_WRAPPER } from './constants';
import { ENABLE_ARBEIDSFLATE, useFeatureToggles } from './core/api/feature-toggles';
import { useHentFnrFraUrl } from './hooks/useHentFnrFraUrl';
import { useInitialBrukerfilter } from './hooks/useInitialBrukerfilter';
import RoutesConfig from './RoutesConfig';
import { ErrorFallback } from './utils/ErrorFallback';

// Trengs for at tab og fokus ikke skal gå utenfor modal når den er åpen.
Modal.setAppElement?.(`#${MODAL_ACCESSIBILITY_WRAPPER}`);

const queryClient = new QueryClient({ defaultOptions: { queries: { refetchOnWindowFocus: false } } });

function AppWrapper() {
  const features = useFeatureToggles();
  useInitialBrukerfilter();

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
      <div className="app__container">
        <div className={APPLICATION_NAME}>
          <QueryClientProvider client={queryClient}>
            <Router>
              <Routes>
                <Route path=":fnr/*" element={<AppWrapper />} />
                <Route path="*" element={<Navigate to={`${fnr}`} />}>
                  {/* Fallback-rute dersom ingenting matcher. Returner bruker til startside */}
                </Route>
              </Routes>
            </Router>
            <ToastContainer
              position="top-right"
              autoClose={5000}
              hideProgressBar={false}
              newestOnTop
              closeOnClick
              rtl={false}
              pauseOnFocusLoss
              draggable
              pauseOnHover
            />
            <ReactQueryDevtools initialIsOpen={false} />
            <Feedback />
          </QueryClientProvider>
        </div>
      </div>
    </ErrorBoundary>
  );
}

export default App;
