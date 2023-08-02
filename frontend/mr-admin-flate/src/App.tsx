import { initializeFaro } from "@grafana/faro-web-sdk";
import { Alert, BodyShort, Modal } from "@navikt/ds-react";
import { NavAnsattRolle } from "mulighetsrommet-api-client";
import { Route, Routes } from "react-router-dom";
import { Forside } from "./Forside";
import IkkeAutentisertApp from "./IkkeAutentisertApp";
import { useHentAnsatt } from "./api/ansatt/useHentAnsatt";
import { Laster } from "./components/laster/Laster";
import { ErrorPage } from "./pages/ErrorPage";
import { AvtalerPage } from "./pages/avtaler/AvtalerPage";
import { DetaljerAvtalePage } from "./pages/avtaler/DetaljerAvtalePage";
import { DetaljerTiltakstypePage } from "./pages/tiltakstyper/DetaljerTiltakstypePage";
import { TiltakstyperPage } from "./pages/tiltakstyper/TiltakstyperPage";
import { TiltaksgjennomforingerPage } from "./pages/tiltaksgjennomforinger/TiltaksgjennomforingerPage";
import { DetaljerTiltaksgjennomforingerPage } from "./pages/tiltaksgjennomforinger/DetaljerTiltaksgjennomforingerPage";
import { NotifikasjonerPage } from "./pages/notifikasjoner/NotifikasjonerPage";
import AvtaleSkjemaPage from "./components/avtaler/AvtaleSkjemaPage";
import TiltaksgjennomforingSkjemaPage from "./components/tiltaksgjennomforinger/TiltaksgjennomforingSkjemaPage";

if (import.meta.env.PROD) {
  initializeFaro({
    url: import.meta.env.VITE_FARO_URL || "http://localhost:12347/collect",
    app: {
      name: "mr-admin-flate",
    },
  });
}

// Trengs for at tab og fokus ikke skal gå utenfor modal når den er åpen.
Modal.setAppElement?.(`#root`);

export function App() {
  const { data: ansatt, isLoading: ansattIsLoading, error } = useHentAnsatt();

  if (error) {
    return (
      <main>
        <Alert variant="error">
          <BodyShort>
            Vi klarte ikke hente brukerinformasjon. Prøv igjen senere.
          </BodyShort>
          <pre>{JSON.stringify(error, null, 2)}</pre>
        </Alert>
      </main>
    );
  }

  if (!ansatt || ansattIsLoading) {
    return (
      <main>
        <Laster tekst="Laster..." size="xlarge" />
      </main>
    );
  }

  if (
    !ansatt.roller?.some(
      (rolle) =>
        rolle === NavAnsattRolle.BETABRUKER ||
        rolle === NavAnsattRolle.TEAM_MULIGHETSROMMET,
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
        path="avtaler/skjema"
        element={<AvtaleSkjemaPage />}
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
        path="tiltaksgjennomforinger/skjema"
        element={<TiltaksgjennomforingSkjemaPage />}
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
