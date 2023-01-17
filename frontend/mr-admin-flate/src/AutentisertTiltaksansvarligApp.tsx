import { Navigate, Route, Routes } from "react-router-dom";
import { RootLayout } from "./layouts/RootLayout";
import { EnhetsoversiktPage } from "./pages/enhet/EnhetsoversiktPage";
import { ErrorPage } from "./pages/ErrorPage";
import { TiltaksgjennomforingPage } from "./pages/tiltaksgjennomforinger/DetaljerTiltaksgjennomforingPage";
import { MineTiltaksgjennomforingerPage } from "./pages/mine/MineTiltaksgjennomforingerPage";
import { Ansatt } from "mulighetsrommet-api-client";
import { OpprettTiltaksgjennomforing } from "./pages/tiltaksgjennomforinger/opprett-tiltaksgjennomforinger/OpprettTiltaksgjennomforingPage";
import { useSideForNavigering } from "./hooks/useSideForNavigering";
import { TiltaksgjennomforingerPage } from "./pages/tiltaksgjennomforinger/TiltaksgjennomforingerPage"

export interface Props {
  ansatt: Ansatt;
}

export default function AutentisertTiltaksansvarligApp({ ansatt }: Props) {
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
        path="opprett"
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
