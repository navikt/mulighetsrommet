import { Alert, BodyShort, Heading } from "@navikt/ds-react";
import { Route, Routes } from "react-router-dom";
import { useHentAnsatt } from "./api/administrator/useHentAdministrator";
import { useFeatureToggles } from "./api/features/feature-toggles";
import { Laster } from "./components/Laster";
import { Forside } from "./Forside";
import IkkeAutentisertApp from "./IkkeAutentisertApp";
import { RootLayout } from "./layouts/RootLayout";
import { AvtalerPage } from "./pages/avtaler/AvtalerPage";
import { DetaljerAvtalePage } from "./pages/avtaler/DetaljerAvtalePage";
import { ErrorPage } from "./pages/ErrorPage";
import { DetaljerTiltakstypePage } from "./pages/tiltakstyper/DetaljerTiltakstypePage";
import { TiltakstyperPage } from "./pages/tiltakstyper/TiltakstyperPage";

export function App() {
  const optionalAnsatt = useHentAnsatt();
  const { data, isLoading } = useFeatureToggles();

  if (!data?.["mulighetsrommet.enable-admin-flate"] && !isLoading) {
    return (
      <Heading data-testid="admin-heading" size="xlarge">
        Admin-flate er skrudd av 💤
      </Heading>
    );
  }

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

  if (!optionalAnsatt.data && optionalAnsatt.isLoading) {
    return (
      <main>
        <Laster tekst="Laster..." size="xlarge" />
      </main>
    );
  }

  if (optionalAnsatt?.data?.tilganger.length === 0) {
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
        path="avtaler/"
        element={
          <RootLayout>
            <AvtalerPage />
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
