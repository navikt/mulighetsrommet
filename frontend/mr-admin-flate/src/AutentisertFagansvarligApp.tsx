import { Route, Routes } from "react-router-dom";
import { RootLayout } from "./layouts/RootLayout";
import { ErrorPage } from "./pages/ErrorPage";
import { TiltakstypePage } from "./pages/TiltakstypePage";
import { OversiktTiltakstyper } from "./pages/OversiktTiltakstyper";
import { ForsideFagansvarlig } from "./ForsideFagansvarlig";
import { TiltaksgjennomforingPage } from "./pages/TiltaksgjennomforingPage";

export default function AutentisertFagansvarligApp() {
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
      <Route
        path="tiltaksgjennomforing/:tiltaksgjennomforingId"
        element={
          <RootLayout>
            <TiltaksgjennomforingPage fagansvarlig />
          </RootLayout>
        }
        errorElement={<ErrorPage />}
      />
    </Routes>
  );
}
