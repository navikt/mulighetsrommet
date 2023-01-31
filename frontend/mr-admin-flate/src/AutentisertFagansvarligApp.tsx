import { Navigate, Route, Routes } from "react-router-dom";
import { RootLayout } from "./layouts/RootLayout";
import { ErrorPage } from "./pages/ErrorPage";
import { TiltaksgjennomforingPage } from "./pages/tiltaksgjennomforinger/DetaljerTiltaksgjennomforingPage";
import { OpprettTiltaksgruppe } from "./pages/tiltaksgrupper/opprett-tiltaksgrupper/OpprettTiltaksgruppePage";
import { TiltaksgruppeDetaljerPage } from "./pages/tiltaksgrupper/TiltaksgruppeDetaljerPage";
import { TiltaksgrupperPage } from "./pages/tiltaksgrupper/TiltaksgrupperPage";
import { DetaljerTiltakstypePage } from "./pages/tiltakstyper/DetaljerTiltakstypePage";
import { TiltakstyperPage } from "./pages/tiltakstyper/TiltakstyperPage";

export default function AutentisertFagansvarligApp() {
  return (
    <Routes>
      <Route
        index
        element={
          <RootLayout>
            <TiltakstyperPage />
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
        path="grupper"
        element={
          <RootLayout>
            <TiltaksgrupperPage />
          </RootLayout>
        }
        errorElement={<ErrorPage />}
      />
      <Route
        path="grupper/opprett"
        element={
          <RootLayout>
            <OpprettTiltaksgruppe />
          </RootLayout>
        }
        errorElement={<ErrorPage />}
      />
      <Route
        path="grupper/:tiltaksgruppeId"
        element={
          <RootLayout>
            <TiltaksgruppeDetaljerPage />
          </RootLayout>
        }
        errorElement={<ErrorPage />}
      />
      {/* <Route
        path="tiltakstyper/opprett"
        element={
          <RootLayout>
            <OpprettTiltakstype />
          </RootLayout>
        }
        errorElement={<ErrorPage />}
      /> */}
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
        path="tiltakstyper/:tiltakstypeId/tiltaksgjennomforing/:tiltaksgjennomforingId"
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
