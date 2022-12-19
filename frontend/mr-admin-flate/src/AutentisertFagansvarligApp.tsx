import { Route, Routes } from "react-router-dom";
import { ForsideFagansvarlig } from "./ForsideFagansvarlig";
import { RootLayout } from "./layouts/RootLayout";
import { ErrorPage } from "./pages/ErrorPage";
import { OversiktTiltakstyper } from "./pages/OversiktTiltakstyper";
import { TiltaksgjennomforingPage } from "./pages/TiltaksgjennomforingPage";
import { TiltakstypePage } from "./pages/TiltakstypePage";

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
        path="tiltakstyper"
        element={
          <RootLayout>
            <OversiktTiltakstyper />
          </RootLayout>
        }
        errorElement={<ErrorPage />}
      />
      <Route
        path="tiltakstyper/:tiltakstypeId"
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
            <TiltaksgjennomforingPage />
          </RootLayout>
        }
        errorElement={<ErrorPage />}
      />
    </Routes>
  );
}
