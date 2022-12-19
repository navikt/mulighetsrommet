import { Route, Routes } from "react-router-dom";
import { ForsideTiltaksansvarlig } from "./ForsideTiltaksansvarlig";
import { RootLayout } from "./layouts/RootLayout";
import { Enhetsoversikt } from "./pages/Enhetsoversikt";
import { ErrorPage } from "./pages/ErrorPage";
import { OversiktTiltaksgjennomforinger } from "./pages/OversiktTiltaksgjennomforinger";
import { TiltaksgjennomforingPage } from "./pages/TiltaksgjennomforingPage";

export default function AutentisertTiltaksansvarligApp() {
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
            <OversiktTiltaksgjennomforinger />
          </RootLayout>
        }
        errorElement={<ErrorPage />}
      />
      <Route
        path="enhet"
        element={
          <RootLayout>
            <Enhetsoversikt />
          </RootLayout>
        }
        errorElement={<ErrorPage />}
      />
      <Route
        path=":tiltaksgjennomforingId"
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
