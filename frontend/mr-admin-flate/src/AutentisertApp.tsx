import { Route, Routes } from "react-router-dom";
import { Forside } from "./Forside";
import { RootLayout } from "./layouts/RootLayout";
import { ErrorPage } from "./pages/ErrorPage";
import { Oversikt } from "./pages/Oversikt";
import { TiltaksgjennomforingPage } from "./pages/TiltaksgjennomforingPage";

export function AutentisertApp() {
  return (
    <Routes>
      <Route
        path="/"
        element={
          <RootLayout>
            <Forside />
          </RootLayout>
        }
        errorElement={<ErrorPage />}
      />
      <Route
        path="oversikt"
        element={
          <RootLayout>
            <Oversikt />
          </RootLayout>
        }
        errorElement={<ErrorPage />}
      />
      <Route
        path="oversikt/:tiltaksgjennomforingId"
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
