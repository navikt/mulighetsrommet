import { Heading } from "@navikt/ds-react";
import { useFeatureToggles } from "./api/features/feature-toggles";
import { Oversikt } from "./pages/Oversikt";
import { Route, Routes } from "react-router-dom";

export function App() {
  const { data, isLoading } = useFeatureToggles();

  if (!data || isLoading) return null;

  if (!data["mulighetsrommet.enable-admin-flate"]) {
    return (
      <Heading data-testid="admin-heading" size="xlarge">
        Admin-flate er skrudd av 💤
      </Heading>
    );
  }

  return (
    <Routes>
      (
      <>
        <Route
          path="/"
          element={
            <main>
              <Heading data-testid="admin-heading" size="xlarge">
                Hello World, admin-flate 💯
              </Heading>
            </main>
          }
        />
        <Route path={"oversikt"} element={<Oversikt />} />
      </>
      )
    </Routes>
  );
}
