import { createRoot } from "react-dom/client";
import { APPLICATION_NAME } from "@/constants";
import { AppContext } from "@/AppContext";
import { BrowserRouter as Router, Link, Route, Routes } from "react-router-dom";
import { NavArbeidsmarkedstiltak } from "@/apps/nav/NavArbeidsmarkedstiltak";
import { PreviewArbeidsmarkedstiltak } from "@/apps/preview/PreviewArbeidsmarkedstiltak";
import { ModiaArbeidsmarkedstiltak } from "@/apps/modia/ModiaArbeidsmarkedstiltak";

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
  const demoContainer = document.getElementById(APPLICATION_NAME);
  if (demoContainer) {
    const root = createRoot(demoContainer);
    root.render(
      <div>
        <Router>
          <div>
            <ul>
              <li>
                <Link to="/arbeidsmarkedstiltak">Modia Arbeidsmarkedstiltak</Link>
              </li>
              <li>
                <Link to="/nav">NAV Arbeidsmarkedstiltak</Link>
              </li>
              <li>
                <Link to="/preview">Preview Arbeidsmarkedstiltak</Link>
              </li>
            </ul>
          </div>

          <hr />

          <Routes>
            <Route
              path="arbeidsmarkedstiltak/*"
              element={
                <AppContext key="modia" contextData={{ fnr: "12345678910", enhet: "0315" }}>
                  <ModiaArbeidsmarkedstiltak />
                </AppContext>
              }
            />
            <Route
              path="nav/*"
              element={
                <AppContext key="nav" contextData={{}}>
                  <NavArbeidsmarkedstiltak />
                </AppContext>
              }
            />
            <Route
              path="preview/*"
              element={
                <AppContext key="preview" contextData={{}}>
                  <PreviewArbeidsmarkedstiltak />
                </AppContext>
              }
            />
          </Routes>
        </Router>
      </div>,
    );
  }
}
