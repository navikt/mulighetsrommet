import { Route, Routes } from "react-router-dom";
import { ForsideTiltaksansvarlig } from "./ForsideTiltaksansvarlig";
import { RootLayout } from "./layouts/RootLayout";
import { EnhetsoversiktPage } from "./pages/enhet/EnhetsoversiktPage";
import { ErrorPage } from "./pages/ErrorPage";
import { TiltaksgjennomforingerPage } from "./pages/tiltaksgjennomforinger/TiltaksgjennomforingerPage";
import { TiltaksgjennomforingPage } from "./pages/tiltaksgjennomforinger/DetaljerTiltaksgjennomforingPage";

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
            <TiltaksgjennomforingerPage />
          </RootLayout>
        }
        errorElement={<ErrorPage />}
      />
      <Route
        path="enhet"
        element={
          <RootLayout>
            <EnhetsoversiktPage />
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
