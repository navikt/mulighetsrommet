import { Route, Routes } from "react-router-dom";
import { ForsideTiltaksansvarlig } from "./pages/forside/ForsideTiltaksansvarlig";
import { RootLayout } from "./layouts/RootLayout";
import { ErrorPage } from "./pages/ErrorPage";
import { TiltaksgjennomforingPage } from "./pages/tiltaksgjennomforinger/DetaljerTiltaksgjennomforingPage";

export default function AutentisertTiltaksansvarligApp() {
  return (
    <Routes>
      <Route
        path="/*"
        element={
          <main>
            <ForsideTiltaksansvarlig />
          </main>
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
