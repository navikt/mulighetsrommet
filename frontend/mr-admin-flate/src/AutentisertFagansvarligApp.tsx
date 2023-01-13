import { Navigate, Route, Routes, useLocation } from "react-router-dom";
import { RootLayout } from "./layouts/RootLayout";
import { ErrorPage } from "./pages/ErrorPage";
import { TiltakstyperPage } from "./pages/tiltakstyper/TiltakstyperPage";
import { TiltaksgjennomforingPage } from "./pages/tiltaksgjennomforinger/DetaljerTiltaksgjennomforingPage";
import { DetaljerTiltakstypePage } from "./pages/tiltakstyper/DetaljerTiltakstypePage";
import { OpprettTiltakstype } from "./pages/tiltakstyper/opprett-tiltakstyper/OpprettTiltakstypePage";
import { useEffect, useState } from "react";

export default function AutentisertFagansvarligApp() {
  const [side, setSide] = useState("/tiltakstyper");
  const location = useLocation();

  useEffect(() => {
    const sidenavn = "/" + location.pathname.split("/")[1];
    return setSide(sidenavn);
  }, [location.pathname]);

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
            <DetaljerTiltakstypePage side={side} />
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
