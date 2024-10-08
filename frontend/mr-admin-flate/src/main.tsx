import "@navikt/ds-css";
import { QueryClient, QueryClientProvider } from "@tanstack/react-query";
import { ReactQueryDevtools } from "@tanstack/react-query-devtools";
import React from "react";
import ReactDOM from "react-dom/client";
import { BrowserRouter as Router } from "react-router-dom";
import { App } from "./App";
import { AdministratorHeader } from "./components/administrator/AdministratorHeader";
import { DemoBanner } from "@/components/demo/DemoBanner";
import "./index.css";
import { isDemo } from "@/environment";
import { ReloadAppErrorBoundary } from "@mr/frontend-common";
import { setupOpenAPIClient } from "@/api/setup-openapi-client";

setupOpenAPIClient({
  base: import.meta.env.VITE_MULIGHETSROMMET_API_BASE ?? "",
  authToken: import.meta.env.VITE_MULIGHETSROMMET_API_AUTH_TOKEN,
});

const queryClient = new QueryClient({
  defaultOptions: {
    queries: {
      throwOnError: true,
      retry: 3,
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
        {isDemo && <DemoBanner />}
        <Router basename={import.meta.env.BASE_URL}>
          <AdministratorHeader />
          <ReloadAppErrorBoundary>
            <App />
          </ReloadAppErrorBoundary>
        </Router>
        <ReactQueryDevtools initialIsOpen={false} />
      </QueryClientProvider>
    </React.StrictMode>,
  );
}
