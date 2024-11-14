/* eslint-disable promise/catch-or-return */
/* eslint-disable promise/always-return */
import { setupOpenAPIClient } from "@/api/setup-openapi-client";
import { DemoBanner } from "@/components/demo/DemoBanner";
import { isDemo } from "@/environment";
import { ReloadAppErrorBoundary } from "@mr/frontend-common";
import "@navikt/ds-css";
import { QueryClient, QueryClientProvider } from "@tanstack/react-query";
import { ReactQueryDevtools } from "@tanstack/react-query-devtools";
import React from "react";
import ReactDOM from "react-dom/client";
import { AppWithRouter } from "./App";
import "./index.css";

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

async function enableMocking() {
  if (import.meta.env.PROD) {
    return;
  }

  if (import.meta.env.VITE_MULIGHETSROMMET_API_MOCK === "true") {
    const worker = await import("./mocks/worker");

    return worker.initializeMockServiceWorker().start({
      onUnhandledRequest: "bypass",
    });
  }
}

enableMocking().then(() => {
  ReactDOM.createRoot(document.getElementById("root") as HTMLElement).render(
    <React.StrictMode>
      <QueryClientProvider client={queryClient}>
        {isDemo && <DemoBanner />}
        <ReloadAppErrorBoundary>
          <AppWithRouter />
        </ReloadAppErrorBoundary>
        <ReactQueryDevtools initialIsOpen={false} />
      </QueryClientProvider>
    </React.StrictMode>,
  );
});
