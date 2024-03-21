import { createRoot } from "react-dom/client";
import { BrowserRouter as Router, Navigate, Route, Routes } from "react-router-dom";
import { APPLICATION_NAME } from "@/constants";
import { NavArbeidsmarkedstiltak } from "./NavArbeidsmarkedstiltak";
import { PreviewArbeidsmarkedstiltak } from "@/apps/nav/PreviewArbeidsmarkedstiltak";
import { ReactQueryProvider } from "@/ReactQueryProvider";
import { initAmplitudeNav } from "@/logging/amplitude";
import { getWebInstrumentations, initializeFaro } from "@grafana/faro-web-sdk";
import "../../App.module.scss";
import "../../index.css";

if (import.meta.env.VITE_FARO_URL) {
  initializeFaro({
    url: import.meta.env.VITE_FARO_URL,
    instrumentations: [...getWebInstrumentations({ captureConsole: true })],
    app: {
      name: "nav-arbeidsmarkedstiltak",
    },
  });
}

initAmplitudeNav();

const container = document.getElementById(APPLICATION_NAME);
if (container) {
  const root = createRoot(container);
  root.render(
    <ReactQueryProvider>
      <Router>
        <Routes>
          <Route path="arbeidsmarkedstiltak/*" element={<NavArbeidsmarkedstiltak />} />
          <Route path="preview/*" element={<PreviewArbeidsmarkedstiltak />} />
          <Route path="*" element={<Navigate replace to="./arbeidsmarkedstiltak" />} />
        </Routes>
      </Router>
    </ReactQueryProvider>,
  );
}
