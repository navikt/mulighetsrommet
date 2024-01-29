import "../polyfill";
import { createRoot } from "react-dom/client";
import { APPLICATION_NAME } from "../constants";
import { NavArbeidsmarkedstiltak } from "../App";
import { AppContext } from "../AppContext";
import { ErrorBoundary } from "react-error-boundary";
import { ErrorFallback } from "../utils/ErrorFallback";
import { BrowserRouter as Router, Navigate, Route, Routes } from "react-router-dom";

if (import.meta.env.VITE_MULIGHETSROMMET_API_MOCK === "true") {
  import("../mock/worker")
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
  const demoContainer = document.getElementById(APPLICATION_NAME);
  if (demoContainer) {
    const root = createRoot(demoContainer);
    root.render(
      <AppContext contextData={{}}>
        <ErrorBoundary FallbackComponent={ErrorFallback}>
          <Router>
            <Routes>
              <Route path="nav/*" element={<NavArbeidsmarkedstiltak />} />
              <Route path="*" element={<Navigate replace to="/nav" />} />
            </Routes>
          </Router>
        </ErrorBoundary>
      </AppContext>,
    );
  }
}
