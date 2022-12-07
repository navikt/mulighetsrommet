import { Route, Routes } from "react-router-dom";
import { ForsideTiltaksansvarlig } from "./ForsideTiltaksansvarlig";
import { RootLayout } from "./layouts/RootLayout";
import { ErrorPage } from "./pages/ErrorPage";
import { Oversikt } from "./pages/Oversikt";
import { TiltaksgjennomforingPage } from "./pages/TiltaksgjennomforingPage";
import { Tiltakstyperoversikt } from "./components/tiltakstyper/Tiltakstyperoversikt";
import { TiltakstypePage } from "./pages/TiltakstypePage";
import { OversiktTiltakstyper } from "./pages/OversiktTiltakstyper";

export function AutentisertTiltaksansvarligApp() {
  return (
    <Routes>
      <Route
        path="/*"
        element={
          <RootLayout>
            <ForsideTiltaksansvarlig />
          </RootLayout>
        }
        errorElement={<ErrorPage />}
      />
      <Route
        path="oversikt"
        element={
          <RootLayout>
            <Oversikt />
          </RootLayout>
        }
        errorElement={<ErrorPage />}
      />
      <Route
        path="oversikt/:tiltaksgjennomforingId"
        element={
          <RootLayout>
            <TiltaksgjennomforingPage />
          </RootLayout>
        }
        errorElement={<ErrorPage />}
      />
    </Routes>
  );
}
