import { createRoot } from "react-dom/client";
import { BrowserRouter as Router, Navigate, Route, Routes } from "react-router";
import { APPLICATION_NAME } from "@/constants";
import { NavArbeidsmarkedstiltak } from "./NavArbeidsmarkedstiltak";
import { PreviewArbeidsmarkedstiltak } from "@/apps/nav/PreviewArbeidsmarkedstiltak";
import { ReactQueryProvider } from "@/ReactQueryProvider";
import { getWebInstrumentations, initializeFaro } from "@grafana/faro-web-sdk";
import "../../index.css";
import { OmArbeidsmarkedstiltak } from "./OmArbeidsmarkedstiltak";

if (import.meta.env.VITE_FARO_URL) {
  initializeFaro({
    url: import.meta.env.VITE_FARO_URL,
    instrumentations: [...getWebInstrumentations({ captureConsole: true })],
    app: {
      name: "nav-arbeidsmarkedstiltak",
    },
    isolate: true,
  });
}

const container = document.getElementById(APPLICATION_NAME);
if (container) {
  const root = createRoot(container);
  root.render(
    <ReactQueryProvider>
      <Router>
        <Routes>
          <Route path="arbeidsmarkedstiltak/*" element={<NavArbeidsmarkedstiltak />} />
          <Route path="preview/*" element={<PreviewArbeidsmarkedstiltak />} />
          <Route path="nav/no" element={<OmArbeidsmarkedstiltak />} />
          <Route path="*" element={<Navigate replace to="./arbeidsmarkedstiltak" />} />
        </Routes>
      </Router>
    </ReactQueryProvider>,
  );
}
