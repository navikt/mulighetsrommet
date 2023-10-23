import "@navikt/ds-css";
import { Alert, BodyShort, Button, Heading } from "@navikt/ds-react";
import { QueryClient, QueryClientProvider } from "@tanstack/react-query";
import { ReactQueryDevtools } from "@tanstack/react-query-devtools";
import { PORTEN } from "mulighetsrommet-frontend-common/constants";
import React from "react";
import ReactDOM from "react-dom/client";
import { ErrorBoundary, FallbackProps } from "react-error-boundary";
import { BrowserRouter as Router, Link } from "react-router-dom";
import "react-toastify/dist/ReactToastify.css";
import { App } from "./App";
import { AdministratorHeader } from "./components/administrator/AdministratorHeader";
import { MiljoBanner } from "./components/miljobanner/MiljoBanner";
import "./index.css";
import { ApiError } from "mulighetsrommet-api-client";
import { resolveErrorMessage } from "./api/errors";

const queryClient = new QueryClient({
  defaultOptions: {
    queries: {
      useErrorBoundary: true,
      refetchOnWindowFocus: import.meta.env.PROD,
      retry: import.meta.env.PROD,
    },
  },
});

if (import.meta.env.VITE_MULIGHETSROMMET_API_MOCK === "true") {
  import("./mocks/worker")
    .then(({ initializeMockServiceWorker }) => {
      return initializeMockServiceWorker();
    })
    .then(render)
    .catch((error) => {
      // eslint-disable-next-line no-console
      console.error("Error occurred while initializing MSW", error);
    });
} else {
  render();
}

function render() {
  ReactDOM.createRoot(document.getElementById("root") as HTMLElement).render(
    <React.StrictMode>
      <QueryClientProvider client={queryClient}>
        <MiljoBanner />
        <Router basename={import.meta.env.BASE_URL}>
          <ErrorBoundary FallbackComponent={ErrorFallback}>
            <AdministratorHeader />
            <App />
          </ErrorBoundary>
        </Router>
        <ReactQueryDevtools initialIsOpen={false} />
      </QueryClientProvider>
    </React.StrictMode>,
  );
}

export function ErrorFallback({ error }: FallbackProps) {
  const heading = error instanceof ApiError ? resolveErrorMessage(error) : error.message;

  if ((error as ApiError).status === 401) {
    return (
      <div className="error">
        <Alert variant="error">
          <Heading size="medium" level="2">
            Autentiseringsfeil
          </Heading>
          <BodyShort>Vennligst logg inn på nytt</BodyShort>
          <Button size="small" onClick={() => window.location.reload()}>
            Logg inn
          </Button>
        </Alert>
      </div>
    );
  }

  return (
    <div className="error">
      <Alert variant="error">
        <Heading size="medium" level="2">
          {heading || "Det oppsto dessverre en feil"}
        </Heading>
        <BodyShort>
          Hvis problemet vedvarer opprett en sak via <a href={PORTEN}>Porten</a>.
        </BodyShort>
        <Link to="/" reloadDocument className="error-link">
          Ta meg til forsiden og prøv igjen
        </Link>
      </Alert>
    </div>
  );
}
