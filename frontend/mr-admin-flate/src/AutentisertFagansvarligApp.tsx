import { Route, Routes } from "react-router-dom";
import { ForsideTiltaksansvarlig } from "./ForsideTiltaksansvarlig";
import { RootLayout } from "./layouts/RootLayout";
import { ErrorPage } from "./pages/ErrorPage";
import { OversiktTiltaksgjennomforinger } from "./pages/OversiktTiltaksgjennomforinger";
import { TiltaksgjennomforingPage } from "./pages/TiltaksgjennomforingPage";
import { Tiltakstyperoversikt } from "./components/tiltakstyper/Tiltakstyperoversikt";
import { TiltakstypePage } from "./pages/TiltakstypePage";
import { OversiktTiltakstyper } from "./pages/OversiktTiltakstyper";
import { ForsideFagansvarlig } from "./ForsideFagansvarlig";

export function AutentisertFagansvarligApp() {
  return (
    <Routes>
      <Route
        path="/*"
        element={
          <RootLayout>
            <ForsideFagansvarlig />
          </RootLayout>
        }
        errorElement={<ErrorPage />}
      />
      <Route
        path="oversikt"
        element={
          <RootLayout>
            <OversiktTiltakstyper />
          </RootLayout>
        }
        errorElement={<ErrorPage />}
      />
      <Route
        path="oversikt/:tiltakstypeId"
        element={
          <RootLayout>
            <TiltakstypePage />
          </RootLayout>
        }
        errorElement={<ErrorPage />}
      />
    </Routes>
  );
}
