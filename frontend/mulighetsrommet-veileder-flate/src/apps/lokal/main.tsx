import { createRoot } from "react-dom/client";
import { APPLICATION_NAME } from "@/constants";
import { ModiaContext } from "@/apps/modia/ModiaContext";
import { BrowserRouter as Router, Link, Route, Routes } from "react-router-dom";
import { NavArbeidsmarkedstiltak } from "@/apps/nav/NavArbeidsmarkedstiltak";
import { PreviewArbeidsmarkedstiltak } from "@/apps/nav/PreviewArbeidsmarkedstiltak";
import { ModiaArbeidsmarkedstiltak } from "@/apps/modia/ModiaArbeidsmarkedstiltak";
import { ReactQueryProvider } from "@/ReactQueryProvider";
import "../../index.css";
import { setupOpenAPIClient } from "@/api/setup-openapi-client";

setupOpenAPIClient({
  base: import.meta.env.VITE_MULIGHETSROMMET_API_BASE ?? "",
  authToken: import.meta.env.VITE_MULIGHETSROMMET_API_AUTH_TOKEN,
});

if (import.meta.env.VITE_MULIGHETSROMMET_API_MOCK === "true") {
  import("../../mock/worker")
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
  const container = document.getElementById(APPLICATION_NAME);
  if (container) {
    const root = createRoot(container);
    root.render(
      <div>
        <Router>
          <ul>
            <li>
              <Link to="/arbeidsmarkedstiltak">Modia Arbeidsmarkedstiltak</Link>
            </li>
            <li>
              <Link to="/nav">Nav Arbeidsmarkedstiltak</Link>
            </li>
            <li>
              <Link to="/preview">Preview Arbeidsmarkedstiltak</Link>
            </li>
          </ul>
          <hr />

          <Routes>
            <Route
              path="arbeidsmarkedstiltak/*"
              element={
                <ModiaContext key="modia" contextData={{ fnr: "12345678910", enhet: "0315" }}>
                  <ModiaArbeidsmarkedstiltak />
                </ModiaContext>
              }
            />
            <Route
              path="nav/*"
              element={
                <ReactQueryProvider>
                  <NavArbeidsmarkedstiltak />
                </ReactQueryProvider>
              }
            />
            <Route
              path="preview/*"
              element={
                <ReactQueryProvider>
                  <PreviewArbeidsmarkedstiltak />
                </ReactQueryProvider>
              }
            />
          </Routes>
        </Router>
      </div>,
    );
  }
}
