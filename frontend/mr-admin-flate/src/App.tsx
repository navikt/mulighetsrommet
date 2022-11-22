import { Heading } from "@navikt/ds-react";
import { useFeatureToggles } from "./api/features/feature-toggles";
import { Oversikt } from "./pages/Oversikt";
import {
  BrowserRouter as Router,
  Navigate,
  Route,
  Routes,
} from "react-router-dom";
import { SanityPreview } from "mulighetsrommet-veileder-flate/src/views/Preview/SanityPreview";
import ViewTiltakstypeOversikt from "mulighetsrommet-veileder-flate/src/views/tiltaksgjennomforing-oversikt/ViewTiltakstypeOversikt";
import ViewTiltaksgjennomforingDetaljer from "mulighetsrommet-veileder-flate/src/views/tiltaksgjennomforing-detaljer/ViewTiltaksgjennomforingDetaljer";

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
