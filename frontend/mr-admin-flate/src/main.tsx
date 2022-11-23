import "@navikt/ds-css";
import "@navikt/ds-css-internal";
import { QueryClient, QueryClientProvider } from "@tanstack/react-query";
import { ReactQueryDevtools } from "@tanstack/react-query-devtools";
import React from "react";
import ReactDOM from "react-dom/client";
import { BrowserRouter as Router, Route, Routes } from "react-router-dom";
import { App } from "./App";
import { AdministratorHeader } from "./components/AdministratorHeader";
import "./index.css";
import { RootLayout } from "./layouts/RootLayout";
import { ErrorPage } from "./pages/ErrorPage";
import { Oversikt } from "./pages/Oversikt";
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
                <Oversikt />
              </RootLayout>
            }
            path="oversikt"
            errorElement={<ErrorPage />}
          />
          <Route
            element={
              <RootLayout>
                <TiltaksgjennomforingPage />
              </RootLayout>
            }
            path="oversikt/:tiltaksgjennomforingId"
            errorElement={<ErrorPage />}
          />
        </Routes>
      </Router>
      <ReactQueryDevtools initialIsOpen={false} />
    </QueryClientProvider>
  </React.StrictMode>
);
