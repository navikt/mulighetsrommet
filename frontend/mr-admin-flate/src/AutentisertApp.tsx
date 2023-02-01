import { Routes, Route, Navigate } from "react-router-dom";
import { useSideForNavigering } from "./hooks/useSideForNavigering";
import { RootLayout } from "./layouts/RootLayout";
import { EnhetsoversiktPage } from "./pages/enhet/EnhetsoversiktPage";
import { ErrorPage } from "./pages/ErrorPage";
import { MineTiltaksgjennomforingerPage } from "./pages/mine/MineTiltaksgjennomforingerPage";
import { TiltaksgjennomforingPage } from "./pages/tiltaksgjennomforinger/DetaljerTiltaksgjennomforingPage";
import { OpprettTiltaksgjennomforing } from "./pages/tiltaksgjennomforinger/opprett-tiltaksgjennomforinger/OpprettTiltaksgjennomforingPage";
import { TiltaksgjennomforingerPage } from "./pages/tiltaksgjennomforinger/TiltaksgjennomforingerPage";
import { OpprettTiltaksgruppe } from "./pages/tiltaksgrupper/opprett-tiltaksgrupper/OpprettTiltaksgruppePage";
import { TiltaksgruppeDetaljerPage } from "./pages/tiltaksgrupper/TiltaksgruppeDetaljerPage";
import { TiltaksgrupperPage } from "./pages/tiltaksgrupper/TiltaksgrupperPage";
import { DetaljerTiltakstypePage } from "./pages/tiltakstyper/DetaljerTiltakstypePage";
import { TiltakstyperPage } from "./pages/tiltakstyper/TiltakstyperPage";

export function AutentisertApp() {
  const side = useSideForNavigering();
  return (
    <Routes>
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
      <Route
        path="mine"
        element={
          <RootLayout>
            <MineTiltaksgjennomforingerPage />
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
        path="gjennomforinger"
        element={
          <RootLayout>
            <TiltaksgjennomforingerPage />
          </RootLayout>
        }
        errorElement={<ErrorPage />}
      />
      <Route
        path={`${side}/tiltaksgjennomforing/:tiltaksgjennomforingId`}
        element={
          <RootLayout>
            <TiltaksgjennomforingPage />
          </RootLayout>
        }
        errorElement={<ErrorPage />}
      />
      <Route
        path="opprett-tiltaksgjennomforing"
        element={
          <RootLayout>
            <OpprettTiltaksgjennomforing />
          </RootLayout>
        }
        errorElement={<ErrorPage />}
      />
      <Route path="/" element={<Navigate to="mine" />} />
    </Routes>
  );
}
