import "@navikt/ds-css";
import React from "react";
import ReactDOM from "react-dom/client";
import { BrowserRouter as Router, Route, Routes } from "react-router-dom";
import "./index.css";
import { RootLayout } from "./layouts/RootLayout";
import { ErrorPage } from "./pages/ErrorPage";
import { QueryClient, QueryClientProvider } from "@tanstack/react-query";
import { ReactQueryDevtools } from "@tanstack/react-query-devtools";
import { Oversikt } from "./pages/Oversikt";

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
        <Routes>
          <Route
            element={<RootLayout />}
            path="*"
            errorElement={<ErrorPage />}
          />
        </Routes>
      </Router>
      <ReactQueryDevtools initialIsOpen={false} />
    </QueryClientProvider>
  </React.StrictMode>
);
