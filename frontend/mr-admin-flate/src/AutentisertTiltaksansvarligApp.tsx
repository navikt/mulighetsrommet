import { Navigate, Route, Routes } from "react-router-dom";
import { useSideForNavigering } from "./hooks/useSideForNavigering";
import { RootLayout } from "./layouts/RootLayout";
import { EnhetsoversiktPage } from "./pages/enhet/EnhetsoversiktPage";
import { ErrorPage } from "./pages/ErrorPage";
import { MineTiltaksgjennomforingerPage } from "./pages/mine/MineTiltaksgjennomforingerPage";
import { TiltaksgjennomforingPage } from "./pages/tiltaksgjennomforinger/DetaljerTiltaksgjennomforingPage";
import { OpprettTiltaksgjennomforing } from "./pages/tiltaksgjennomforinger/opprett-tiltaksgjennomforinger/OpprettTiltaksgjennomforingPage";
import { TiltaksgjennomforingerPage } from "./pages/tiltaksgjennomforinger/TiltaksgjennomforingerPage";

export default function AutentisertTiltaksansvarligApp() {
  const side = useSideForNavigering();

  return (
    <Routes>
      <Route
        path="/mine"
        element={
          <RootLayout>
            <MineTiltaksgjennomforingerPage />
          </RootLayout>
        }
        errorElement={<ErrorPage />}
      />
      <Route
        path="/enhet"
        element={
          <RootLayout>
            <EnhetsoversiktPage />
          </RootLayout>
        }
        errorElement={<ErrorPage />}
      />
      <Route
        path="/oversikt"
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
