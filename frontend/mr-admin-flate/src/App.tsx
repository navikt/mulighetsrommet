import { Alert, BodyShort } from "@navikt/ds-react";
import { Route, Routes } from "react-router-dom";
import { useHentAnsatt } from "./api/administrator/useHentAdministrator";
import { Laster } from "./components/Laster";
import { Forside } from "./Forside";
import IkkeAutentisertApp from "./IkkeAutentisertApp";
import { RootLayout } from "./layouts/RootLayout";
import { DetaljerAvtalePage } from "./pages/avtaler/DetaljerAvtalePage";
import { ErrorPage } from "./pages/ErrorPage";
import { DetaljerTiltakstypePage } from "./pages/tiltakstyper/DetaljerTiltakstypePage";
import { TiltakstyperPage } from "./pages/tiltakstyper/TiltakstyperPage";

export function App() {
  const optionalAnsatt = useHentAnsatt();

  if (optionalAnsatt.error) {
    return (
      <main>
        <Alert variant="error">
          <BodyShort>
            Vi klarte ikke hente brukerinformasjon. Prøv igjen senere.
          </BodyShort>
          <pre>{JSON.stringify(optionalAnsatt?.error, null, 2)}</pre>
        </Alert>
      </main>
    );
  }

  if (optionalAnsatt.isFetching || !optionalAnsatt.data) {
    return (
      <main>
        <Laster tekst="Laster..." size="xlarge" />
      </main>
    );
  }

  if (optionalAnsatt?.data.tilganger.length === 0) {
    return <IkkeAutentisertApp />;
  }

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
        path="tiltakstyper/:tiltakstypeId"
        element={
          <RootLayout>
            <DetaljerTiltakstypePage />
          </RootLayout>
        }
        errorElement={<ErrorPage />}
      />
      <Route
        path="avtaler/:avtaleId"
        element={
          <RootLayout>
            <DetaljerAvtalePage />
          </RootLayout>
        }
        errorElement={<ErrorPage />}
      />
      <Route index element={<Forside />} />
    </Routes>
  );
}
