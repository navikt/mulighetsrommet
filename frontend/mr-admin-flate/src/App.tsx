import { Alert, BodyShort, Heading } from "@navikt/ds-react";
import { Route, Routes } from "react-router-dom";
import { useFeatureToggles } from "./api/features/feature-toggles";
import { useHentAnsatt } from "./api/ansatt/useHentAnsatt";
import { Laster } from "./components/laster/Laster";
import { Forside } from "./Forside";
import IkkeAutentisertApp from "./IkkeAutentisertApp";
import { AvtalerPage } from "./pages/avtaler/AvtalerPage";
import { DetaljerAvtalePage } from "./pages/avtaler/DetaljerAvtalePage";
import { ErrorPage } from "./pages/ErrorPage";
import { DetaljerTiltakstypePage } from "./pages/tiltakstyper/DetaljerTiltakstypePage";
import { TiltakstyperPage } from "./pages/tiltakstyper/TiltakstyperPage";
import { TiltaksgjennomforingerPage } from "./pages/tiltaksgjennomforinger/TiltaksgjennomforingerPage";
import { DetaljerTiltaksgjennomforingerPage } from "./pages/tiltaksgjennomforinger/DetaljerTiltaksgjennomforingerPage";
import { NotifikasjonerPage } from "./pages/notifikasjoner/NotifikasjonerPage";
import { initializeFaro } from "@grafana/faro-web-sdk";

if (import.meta.env.PROD) {
  initializeFaro({
    url: import.meta.env.VITE_FARO_URL || "http://localhost:12347/collect",
    app: {
      name: "mr-admin-flate",
    },
  });
}

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

  if (
    !optionalAnsatt?.data?.tilganger.some(
      (tilgang) => tilgang === "BETABRUKER" || tilgang === "UTVIKLER_VALP"
    )
  ) {
    return <IkkeAutentisertApp />;
  }

  return (
    <Routes>
      <Route
        path="tiltakstyper"
        element={<TiltakstyperPage />}
        errorElement={<ErrorPage />}
      />
      <Route
        path="tiltakstyper/:tiltakstypeId"
        element={<DetaljerTiltakstypePage />}
        errorElement={<ErrorPage />}
      />
      <Route
        path="avtaler/"
        element={<AvtalerPage />}
        errorElement={<ErrorPage />}
      />
      <Route
        path="avtaler/:avtaleId"
        element={<DetaljerAvtalePage />}
        errorElement={<ErrorPage />}
      />
      <Route
        path="tiltaksgjennomforinger/"
        element={<TiltaksgjennomforingerPage />}
        errorElement={<ErrorPage />}
      />
      <Route
        path="tiltaksgjennomforinger/:tiltaksgjennomforingId"
        element={<DetaljerTiltaksgjennomforingerPage />}
        errorElement={<ErrorPage />}
      />
      <Route
        path="notifikasjoner"
        element={<NotifikasjonerPage />}
        errorElement={<ErrorPage />}
      />

      <Route index element={<Forside />} />
    </Routes>
  );
}
