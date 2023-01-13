import { Route, Routes } from "react-router-dom";
import { ForsideTiltaksansvarlig } from "./ForsideTiltaksansvarlig";
import { RootLayout } from "./layouts/RootLayout";
import { EnhetsoversiktPage } from "./pages/enhet/EnhetsoversiktPage";
import { ErrorPage } from "./pages/ErrorPage";
import { TiltaksgjennomforingerPage } from "./pages/tiltaksgjennomforinger/TiltaksgjennomforingerPage";
import { TiltaksgjennomforingPage } from "./pages/tiltaksgjennomforinger/DetaljerTiltaksgjennomforingPage";
import { MineTiltaksgjennomforingerPage } from "./pages/mine/MineTiltaksgjennomforingerPage";
import { Ansatt } from "mulighetsrommet-api-client";

export interface Props {
  ansatt: Ansatt;
}

export default function AutentisertTiltaksansvarligApp({ ansatt }: Props) {
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
            <EnhetsoversiktPage ansatt={ansatt} />
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
