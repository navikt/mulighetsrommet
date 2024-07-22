import { useHentAnsatt } from "@/api/ansatt/useHentAnsatt";
import { getWebInstrumentations, initializeFaro } from "@grafana/faro-web-sdk";
import { Alert, BodyShort } from "@navikt/ds-react";
import { NavAnsattRolle } from "mulighetsrommet-api-client";
import { Route, Routes } from "react-router-dom";
import { Forside } from "./Forside";
import IkkeAutentisertApp from "./IkkeAutentisertApp";
import { Laster } from "./components/laster/Laster";
import { Notifikasjonsliste } from "./components/notifikasjoner/Notifikasjonsliste";
import { initializeAmplitude } from "./logging/amplitude";
import { ErrorPage } from "./pages/ErrorPage";
import { ArrangorPageContainer } from "./pages/arrangor/ArrangorPageContainer";
import { ArrangorerPage } from "./pages/arrangor/ArrangorerPage";
import { AvtaleInfo } from "./pages/avtaler/AvtaleInfo";
import { AvtalePage } from "./pages/avtaler/AvtalePage";
import AvtaleSkjemaPage from "./pages/avtaler/AvtaleSkjemaPage";
import { AvtalerPage } from "./pages/avtaler/AvtalerPage";
import { NotifikasjonerPage } from "./pages/notifikasjoner/NotifikasjonerPage";
import { TiltaksgjennomforingInfo } from "./pages/tiltaksgjennomforinger/TiltaksgjennomforingInfo";
import { TiltaksgjennomforingPage } from "./pages/tiltaksgjennomforinger/TiltaksgjennomforingPage";
import TiltaksgjennomforingSkjemaPage from "./pages/tiltaksgjennomforinger/TiltaksgjennomforingSkjemaPage";
import { TiltaksgjennomforingerForAvtalePage } from "./pages/tiltaksgjennomforinger/TiltaksgjennomforingerForAvtalePage";
import { TiltaksgjennomforingerPage } from "./pages/tiltaksgjennomforinger/TiltaksgjennomforingerPage";
import { DetaljerTiltakstypePage } from "./pages/tiltakstyper/DetaljerTiltakstypePage";
import { TiltakstypeInfo } from "./pages/tiltakstyper/TiltakstypeInfo";
import { TiltakstyperPage } from "./pages/tiltakstyper/TiltakstyperPage";
import { AvtalerForTiltakstypePage } from "./pages/tiltakstyper/avtaler/AvtalerForTiltakstypePage";

if (import.meta.env.PROD) {
  initializeFaro({
    url: import.meta.env.VITE_FARO_URL || "http://localhost:12347/collect",
    app: {
      name: "mr-admin-flate",
    },
    instrumentations: [...getWebInstrumentations({ captureConsole: true })],
    isolate: true,
  });
}
initializeAmplitude();

export function App() {
  const { data: ansatt, isLoading: ansattIsLoading, error } = useHentAnsatt();

  if (error) {
    return (
      <main>
        <Alert variant="error">
          <BodyShort>Vi klarte ikke hente brukerinformasjon. Prøv igjen senere.</BodyShort>
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
        rolle === NavAnsattRolle.AVTALER_SKRIV ||
        rolle === NavAnsattRolle.TILTAKSGJENNOMFORINGER_SKRIV ||
        rolle === NavAnsattRolle.TEAM_MULIGHETSROMMET ||
        rolle === NavAnsattRolle.TILTAKADMINISTRASJON_GENERELL,
    )
  ) {
    return <IkkeAutentisertApp />;
  }

  return (
    <Routes>
      <Route path="tiltakstyper" element={<TiltakstyperPage />} errorElement={<ErrorPage />} />
      <Route
        path="tiltakstyper/:tiltakstypeId"
        element={<DetaljerTiltakstypePage />}
        errorElement={<ErrorPage />}
      >
        <Route index element={<TiltakstypeInfo />} errorElement={<ErrorPage />} />
        <Route
          path="avtaler"
          element={<AvtalerForTiltakstypePage />}
          errorElement={<ErrorPage />}
        />
      </Route>
      <Route path="avtaler" element={<AvtalerPage />} errorElement={<ErrorPage />} />
      <Route path="avtaler/:avtaleId" element={<AvtalePage />} errorElement={<ErrorPage />}>
        <Route index element={<AvtaleInfo />} errorElement={<ErrorPage />} />
        <Route
          path="tiltaksgjennomforinger"
          element={<TiltaksgjennomforingerForAvtalePage />}
          errorElement={<ErrorPage />}
        />
      </Route>
      <Route
        path="avtaler/:avtaleId/skjema"
        element={<AvtaleSkjemaPage />}
        errorElement={<ErrorPage />}
      />
      <Route path="avtaler/skjema" element={<AvtaleSkjemaPage />} errorElement={<ErrorPage />} />
      <Route
        path="tiltaksgjennomforinger/skjema"
        element={<TiltaksgjennomforingSkjemaPage />}
        errorElement={<ErrorPage />}
      />
      <Route
        path="tiltaksgjennomforinger/"
        element={<TiltaksgjennomforingerPage />}
        errorElement={<ErrorPage />}
      />
      <Route
        path="avtaler/:avtaleId/tiltaksgjennomforinger/:tiltaksgjennomforingId"
        element={<TiltaksgjennomforingPage />}
        errorElement={<ErrorPage />}
      >
        <Route index element={<TiltaksgjennomforingInfo />} errorElement={<ErrorPage />} />
      </Route>
      <Route
        path="tiltaksgjennomforinger/:tiltaksgjennomforingId"
        element={<TiltaksgjennomforingPage />}
        errorElement={<ErrorPage />}
      >
        <Route index element={<TiltaksgjennomforingInfo />} errorElement={<ErrorPage />} />
      </Route>
      <Route
        path="avtaler/:avtaleId/tiltaksgjennomforinger/:tiltaksgjennomforingId/skjema"
        element={<TiltaksgjennomforingSkjemaPage />}
        errorElement={<ErrorPage />}
      />
      <Route
        path="avtaler/:avtaleId/tiltaksgjennomforinger/skjema"
        element={<TiltaksgjennomforingSkjemaPage />}
        errorElement={<ErrorPage />}
      />
      <Route
        path="tiltaksgjennomforinger/:tiltaksgjennomforingId/skjema"
        element={<TiltaksgjennomforingSkjemaPage />}
        errorElement={<ErrorPage />}
      />
      <Route path="arrangorer" element={<ArrangorerPage />} errorElement={<ErrorPage />} />
      <Route
        path="arrangorer/:arrangorId"
        element={<ArrangorPageContainer />}
        errorElement={<ErrorPage />}
      />
      <Route path="notifikasjoner" element={<NotifikasjonerPage />} errorElement={<ErrorPage />}>
        <Route index element={<Notifikasjonsliste lest={false} />} errorElement={<ErrorPage />} />
        <Route
          path="tidligere"
          element={<Notifikasjonsliste lest={true} />}
          errorElement={<ErrorPage />}
        />
      </Route>
      <Route index element={<Forside />} />
    </Routes>
  );
}
