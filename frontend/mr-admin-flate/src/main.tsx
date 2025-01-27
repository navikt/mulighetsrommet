/* eslint-disable promise/catch-or-return */
/* eslint-disable promise/always-return */
import { DemoBanner } from "@/components/demo/DemoBanner";
import { isDemo } from "@/environment";
import { ReloadAppErrorBoundary } from "@/ErrorBoundary";
import "@navikt/ds-css";
import { QueryClient, QueryClientProvider } from "@tanstack/react-query";
import { ReactQueryDevtools } from "@tanstack/react-query-devtools";
import React from "react";
import ReactDOM from "react-dom/client";
import { AppWithRouter } from "./App";
import { client } from "@mr/api-client-v2";
import "./index.css";
import { ApiError } from "@mr/frontend-common/components/error-handling/errors";
import { v4 as uuidv4 } from "uuid";
import { APPLICATION_NAME } from "@/constants";

const queryClient = new QueryClient({
  defaultOptions: {
    queries: {
      throwOnError: true,
      retry: 3,
    },
  },
});

client.setConfig({
  baseUrl: import.meta.env.VITE_MULIGHETSROMMET_API_BASE ?? "",
});

client.interceptors.response.use(async (response) => {
  if (response.status < 200 || response.status >= 300) {
    let body: unknown;
    try {
      body = await response.json();
    } catch {
      // Do nothing
    }

    throw {
      status: response.status,
      body,
    } as ApiError;
  }
  return response;
});

client.interceptors.request.use((request) => {
  request.headers.set("Accept", "application/json");
  request.headers.set("Nav-Call-Id", uuidv4());
  request.headers.set("Nav-Consumer-Id", APPLICATION_NAME);
  if (import.meta.env.VITE_MULIGHETSROMMET_API_AUTH_TOKEN) {
    request.headers.set(
      "Authorization",
      `Bearer ${import.meta.env.VITE_MULIGHETSROMMET_API_AUTH_TOKEN}`,
    );
  }

  return request;
});

async function enableMocking() {
  if (import.meta.env.VITE_MULIGHETSROMMET_API_MOCK === "true") {
    const worker = await import("./mocks/worker");

    return worker.initializeMockServiceWorker().start({
      onUnhandledRequest: "bypass",
    });
  } else {
    return;
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
