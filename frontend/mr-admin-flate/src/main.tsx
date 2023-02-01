import "@navikt/ds-css";
import "@navikt/ds-css-internal";
import { QueryClient, QueryClientProvider } from "@tanstack/react-query";
import { ReactQueryDevtools } from "@tanstack/react-query-devtools";
import React from "react";
import ReactDOM from "react-dom/client";
import { BrowserRouter as Router } from "react-router-dom";
import { App } from "./App";
import { AdministratorHeader } from "./components/AdministratorHeader";
import { MiljoBanner } from "./components/miljobanner/MiljoBanner";
import "./index.css";

const queryClient = new QueryClient({
  defaultOptions: {
    queries: {
      refetchOnWindowFocus: true, // sett til false for å ta bort refetch ved fokus
    },
  },
});

if (
  process.env.NODE_ENV === "development" ||
  import.meta.env.VITE_INCLUDE_MOCKS === "true"
) {
  import("./mocks/browser").then(({ worker }) => {
    worker.start();
    render();
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
          <AdministratorHeader />
          <App />
        </Router>
        <ReactQueryDevtools initialIsOpen={false} />
      </QueryClientProvider>
    </React.StrictMode>
  );
}
