import React from "react";
import ReactDOM from "react-dom/client";
import { BrowserRouter as Router, Route, Routes } from "react-router-dom";
import { RootLayout } from "./layouts/RootLayout";
import { ErrorPage } from "./pages/ErrorPage";
import { QueryClient, QueryClientProvider } from "@tanstack/react-query";
import { ReactQueryDevtools } from "@tanstack/react-query-devtools";
import "@navikt/ds-css";
import "@navikt/ds-css-internal";
import "./index.css";
import { AdministratorHeader } from "./components/AdministratorHeader";
import { App } from "./App";
import { TiltaksgjennomforingPage } from "./pages/TiltaksgjennomforingPage";

const queryClient = new QueryClient();

if (process.env.NODE_ENV === "development") {
  import("./mocks/browser").then(({ worker }) => {
    worker.start();
  });
}

ReactDOM.createRoot(document.getElementById("root") as HTMLElement).render(
  <React.StrictMode>
    <QueryClientProvider client={queryClient}>
      <Router>
        <AdministratorHeader />
        <Routes>
          <Route
            element={
              <RootLayout>
                <App />
              </RootLayout>
            }
            path="/"
            errorElement={<ErrorPage />}
          />
          <Route
            element={
              <RootLayout>
                <TiltaksgjennomforingPage />
              </RootLayout>
            }
            path="/:tiltaksgjennomforingId"
            errorElement={<ErrorPage />}
          />
        </Routes>
      </Router>
      <ReactQueryDevtools initialIsOpen={false} />
    </QueryClientProvider>
  </React.StrictMode>
);
