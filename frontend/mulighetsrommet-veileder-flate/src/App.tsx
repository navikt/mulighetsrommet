import {Modal} from '@navikt/ds-react';
import {ErrorBoundary} from 'react-error-boundary';
import {QueryClient, QueryClientProvider} from 'react-query';
import {ReactQueryDevtools} from 'react-query/devtools';
import {BrowserRouter as Router} from 'react-router-dom';
import {ToastContainer} from 'react-toastify';
import 'react-toastify/dist/ReactToastify.css';
import './App.less';
import Feedback from './components/feedback/Feedback';
import {APPLICATION_NAME, MODAL_ACCESSIBILITY_WRAPPER} from './constants';
import RoutesConfig from './RoutesConfig';
import {ErrorFallback} from './utils/ErrorFallback';

// Trengs for at tab og fokus ikke skal gå utenfor modal når den er åpen.
Modal.setAppElement?.(`#${MODAL_ACCESSIBILITY_WRAPPER}`);

const queryClient = new QueryClient({ defaultOptions: { queries: { refetchOnWindowFocus: false } } });

function App() {
  return (
    <ErrorBoundary FallbackComponent={ErrorFallback}>
      <div className="app__container">
        <div className={APPLICATION_NAME}>
          <QueryClientProvider client={queryClient}>
            <Router>
              <RoutesConfig />
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
