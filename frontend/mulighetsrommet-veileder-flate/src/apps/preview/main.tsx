import { createRoot } from "react-dom/client";
import { APPLICATION_NAME } from "../../constants";
import { AppContext } from "../../AppContext";
import { BrowserRouter as Router, Navigate, Route, Routes } from "react-router-dom";
import { PreviewArbeidsmarkedstiltak } from "./PreviewArbeidsmarkedstiltak";

const demoContainer = document.getElementById(APPLICATION_NAME);
if (demoContainer) {
  const root = createRoot(demoContainer);
  root.render(
    <AppContext contextData={{}}>
      <Router>
        <Routes>
          <Route path="preview/*" element={<PreviewArbeidsmarkedstiltak />} />
          <Route path="*" element={<Navigate replace to="/preview" />} />
        </Routes>
      </Router>
    </AppContext>,
  );
}
