import { useState } from "react";
import { AppTheme } from "@/constants";
import { ModiaContext } from "@/apps/modia/ModiaContext";
import { BrowserRouter as Router, Link, Route, Routes } from "react-router";
import { NavArbeidsmarkedstiltak } from "@/apps/nav/NavArbeidsmarkedstiltak";
import { PreviewArbeidsmarkedstiltak } from "@/apps/nav/PreviewArbeidsmarkedstiltak";
import { ModiaArbeidsmarkedstiltak } from "@/apps/modia/ModiaArbeidsmarkedstiltak";
import { ReactQueryProvider } from "@/ReactQueryProvider";
import { Switch } from "@navikt/ds-react";

export function LokalApp() {
  const [theme, setTheme] = useState<AppTheme>("light");

  return (
    <div>
      <Router>
        <div className="p-2 flex justify-between items-center">
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
          <Switch
            checked={theme === "dark"}
            onChange={(e) => setTheme(e.target.checked ? "dark" : "light")}
          >
            Mørk modus (for Modia)
          </Switch>
        </div>
        <hr />

        <Routes>
          <Route
            path="arbeidsmarkedstiltak/*"
            element={
              <ModiaContext key="modia" contextData={{ fnr: "12345678910", enhet: "0315" }}>
                <ModiaArbeidsmarkedstiltak theme={theme} />
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
    </div>
  );
}
