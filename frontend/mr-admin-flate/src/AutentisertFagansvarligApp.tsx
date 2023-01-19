import { Navigate, Route, Routes } from "react-router-dom";
import { RootLayout } from "./layouts/RootLayout";
import { ErrorPage } from "./pages/ErrorPage";
import { TiltakstyperPage } from "./pages/tiltakstyper/TiltakstyperPage";
import { TiltaksgjennomforingPage } from "./pages/tiltaksgjennomforinger/DetaljerTiltaksgjennomforingPage";
import { DetaljerTiltakstypePage } from "./pages/tiltakstyper/DetaljerTiltakstypePage";
import { OpprettTiltakstype } from "./pages/tiltakstyper/opprett-tiltakstyper/OpprettTiltakstypePage";
import { TiltaksgrupperPage } from "./pages/tiltaksgrupper/TiltaksgrupperPage";

export default function AutentisertFagansvarligApp() {
  return (
    <Routes>
      <Route
        path="tiltakstyper"
        element={
          <RootLayout fagansvarlig>
            <TiltakstyperPage />
          </RootLayout>
        }
        errorElement={<ErrorPage />}
      />
      <Route
        path="tiltaksgrupper"
        element={
          <RootLayout fagansvarlig>
            <TiltaksgrupperPage />
          </RootLayout>
        }
        errorElement={<ErrorPage />}
      />
      <Route
        path="tiltakstyper/opprett"
        element={
          <RootLayout fagansvarlig>
            <OpprettTiltakstype />
          </RootLayout>
        }
        errorElement={<ErrorPage />}
      />
      <Route
        path="tiltakstyper/:tiltakstypeId"
        element={
          <RootLayout fagansvarlig>
            <DetaljerTiltakstypePage />
          </RootLayout>
        }
        errorElement={<ErrorPage />}
      />
      <Route
        path="tiltakstyper/:tiltakstypeId/tiltaksgjennomforing/:tiltaksgjennomforingId"
        element={
          <RootLayout fagansvarlig>
            <TiltaksgjennomforingPage fagansvarlig />
          </RootLayout>
        }
        errorElement={<ErrorPage />}
      />
      <Route path="/" element={<Navigate to="/tiltakstyper" />} />
    </Routes>
  );
}
