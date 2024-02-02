import { createRoot } from "react-dom/client";
import { BrowserRouter as Router, Navigate, Route, Routes } from "react-router-dom";
import { APPLICATION_NAME } from "@/constants";
import { NavArbeidsmarkedstiltak } from "./NavArbeidsmarkedstiltak";
import { PreviewArbeidsmarkedstiltak } from "@/apps/nav/PreviewArbeidsmarkedstiltak";
import { ReactQueryProvider } from "@/ReactQueryProvider";

const demoContainer = document.getElementById(APPLICATION_NAME);
if (demoContainer) {
  const root = createRoot(demoContainer);
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
