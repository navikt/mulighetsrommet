import {BodyShort, Ingress, Modal} from '@navikt/ds-react';
import {ErrorBoundary} from 'react-error-boundary';
import {QueryClient, QueryClientProvider} from 'react-query';
import {ReactQueryDevtools} from 'react-query/devtools';
import {BrowserRouter as Router} from 'react-router-dom';
import {ToastContainer} from 'react-toastify';
import 'react-toastify/dist/ReactToastify.css';
import './App.less';
import Feedback from './components/feedback/Feedback';
import {Feilmelding} from './components/feilmelding/Feilmelding';
import {APPLICATION_NAME, MODAL_ACCESSIBILITY_WRAPPER} from './constants';
import RoutesConfig from './RoutesConfig';

// Trengs for at tab og fokus ikke skal gå utenfor modal når den er åpen.
Modal.setAppElement?.(`#${MODAL_ACCESSIBILITY_WRAPPER}`);

const queryClient = new QueryClient({ defaultOptions: { queries: { refetchOnWindowFocus: false } } });

function ErrorFallback({ error }: any) {
  let feilmelding = (
    <BodyShort>
      Vi er ikke helt sikre på hva som gikk falt. Du kan gå tilbake, eller{' '}
      <a href="https://jira.adeo.no/plugins/servlet/desk/portal/541/create/1401">ta kontakt i Porten</a> hvis du trenger
      hjelp.
    </BodyShort>
  );

  if (error.status === 404) {
    feilmelding = (
      <BodyShort>
        Beklager, siden kan være slettet eller flyttet, eller det var en feil i lenken som førte deg hit.
      </BodyShort>
    );
  }

  if (error.status === 401 || error.status === 403) {
    feilmelding = (
      <BodyShort>
        Det oppstod en feil under behandlingen av forespørselen din. Ta kontakt med admin hvis problemene vedvarer
      </BodyShort>
    );
  }

  return (
    <Feilmelding>
      <>
        <Ingress>
          Noe gikk galt - Statuskode: {error.status} {error.statusText}
        </Ingress>
        {feilmelding}
        <a href="/">Tilbake til forsiden</a>
      </>
    </Feilmelding>
  );
}

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
