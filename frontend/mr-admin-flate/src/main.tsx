import "@navikt/ds-css";
import "@navikt/ds-css-internal";
import { Alert, Heading } from "@navikt/ds-react";
import { QueryClient, QueryClientProvider } from "@tanstack/react-query";
import { ReactQueryDevtools } from "@tanstack/react-query-devtools";
import React from "react";
import ReactDOM from "react-dom/client";
import { ErrorBoundary, FallbackProps } from "react-error-boundary";
import { Link, BrowserRouter as Router } from "react-router-dom";
import "react-toastify/dist/ReactToastify.css";
import { App } from "./App";
import { AdministratorHeader } from "./components/administrator/AdministratorHeader";
import { MiljoBanner } from "./components/miljobanner/MiljoBanner";
import "./index.css";

const queryClient = new QueryClient({
  defaultOptions: {
    queries: {
      refetchOnWindowFocus: import.meta.env.PROD,
    },
  },
});

if (!import.meta.env.PROD || import.meta.env.VITE_INCLUDE_MOCKS === "true") {
  import("./mocks/browser").then(({ worker }) => {
    worker.start();
    render();
  });
} else {
  render();
}

function ErrorFallback({ error, resetErrorBoundary }: FallbackProps) {
  return (
    <div className="error">
      <Alert variant="error">
        <Heading size="medium" level="2">
          En feil oppsto: {error.message}
        </Heading>
        <Link to="/" onClick={resetErrorBoundary}>
          Ta meg til forsiden og prøv igjen
        </Link>
      </Alert>
    </div>
  );
}

function render() {
  ReactDOM.createRoot(document.getElementById("root") as HTMLElement).render(
    <React.StrictMode>
      <QueryClientProvider client={queryClient}>
        <MiljoBanner />
        <Router basename={import.meta.env.BASE_URL}>
          <AdministratorHeader />
          <ErrorBoundary FallbackComponent={ErrorFallback}>
            <App />
          </ErrorBoundary>
        </Router>
        <ReactQueryDevtools initialIsOpen={false} />
      </QueryClientProvider>
    </React.StrictMode>
  );
}
