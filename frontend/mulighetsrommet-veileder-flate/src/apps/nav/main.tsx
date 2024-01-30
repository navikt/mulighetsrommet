import { createRoot } from "react-dom/client";
import { BrowserRouter as Router, Navigate, Route, Routes } from "react-router-dom";
import { APPLICATION_NAME } from "@/constants";
import { AppContext } from "@/AppContext";
import { NavArbeidsmarkedstiltak } from "./NavArbeidsmarkedstiltak";

const demoContainer = document.getElementById(APPLICATION_NAME);
if (demoContainer) {
  const root = createRoot(demoContainer);
  root.render(
    <AppContext contextData={{}}>
      <Router>
        <Routes>
          <Route path="nav/*" element={<NavArbeidsmarkedstiltak />} />
          <Route path="*" element={<Navigate replace to="/nav" />} />
        </Routes>
      </Router>
    </AppContext>,
  );
}
