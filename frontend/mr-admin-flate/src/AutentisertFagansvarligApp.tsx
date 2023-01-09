import { Route, Routes } from "react-router-dom";
import { ForsideFagansvarlig } from "./ForsideFagansvarlig";
import { RootLayout } from "./layouts/RootLayout";
import { ErrorPage } from "./pages/ErrorPage";
import { TiltakstyperPage } from "./pages/tiltakstyper/TiltakstyperPage";
import { TiltaksgjennomforingPage } from "./pages/tiltaksgjennomforinger/DetaljerTiltaksgjennomforingPage";
import { DetaljerTiltakstypePage } from "./pages/tiltakstyper/DetaljerTiltakstypePage";
import { OpprettTiltakstype } from "./pages/tiltakstyper/opprett-tiltakstyper/OpprettTiltakstypePage";

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
            <TiltakstyperPage />
          </RootLayout>
        }
        errorElement={<ErrorPage />}
      />
      <Route
        path="tiltakstyper/opprett"
        element={
          <RootLayout>
            <OpprettTiltakstype />
          </RootLayout>
        }
        errorElement={<ErrorPage />}
      />
      <Route
        path="tiltakstyper/:tiltakstypeId"
        element={
          <RootLayout>
            <DetaljerTiltakstypePage />
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
