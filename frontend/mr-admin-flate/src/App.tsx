import { initializeFaro } from "@grafana/faro-web-sdk";
import { Alert, BodyShort } from "@navikt/ds-react";
import { NavAnsattRolle, UtkastRequest as Utkast } from "mulighetsrommet-api-client";
import { Route, Routes } from "react-router-dom";
import { Forside } from "./Forside";
import IkkeAutentisertApp from "./IkkeAutentisertApp";
import { useHentAnsatt } from "./api/ansatt/useHentAnsatt";
import AvtaleSkjemaPage from "./components/avtaler/AvtaleSkjemaPage";
import NotaterAvtalePage from "./components/avtaler/NotaterAvtalePage";
import { Avtalefilter } from "./components/filter/Avtalefilter";
import { Tiltaksgjennomforingfilter } from "./components/filter/Tiltaksgjennomforingfilter";
import { Laster } from "./components/laster/Laster";
import { Notifikasjonsliste } from "./components/notifikasjoner/Notifikasjonsliste";
import { AvtaleTabell } from "./components/tabell/AvtaleTabell";
import { TiltaksgjennomforingsTabell } from "./components/tabell/TiltaksgjennomforingsTabell";
import NotaterTiltaksgjennomforingerPage from "./components/tiltaksgjennomforinger/NotaterTiltaksgjennomforingerPage";
import TiltaksgjennomforingSkjemaPage from "./components/tiltaksgjennomforinger/TiltaksgjennomforingSkjemaPage";
import { UtkastListe } from "./components/utkast/Utkastliste";
import { DeltakerListe } from "./microfrontends/team_komet/Deltakerliste";
import { ErrorPage } from "./pages/ErrorPage";
import { AvtaleInfo } from "./pages/avtaler/AvtaleInfo";
import { AvtalerPage } from "./pages/avtaler/AvtalerPage";
import { DetaljerAvtalePage } from "./pages/avtaler/DetaljerAvtalePage";
import { TiltaksgjennomforingerForAvtale } from "./pages/avtaler/tiltaksgjennomforinger/TiltaksgjennomforingerForAvtale";
import { NotifikasjonerPage } from "./pages/notifikasjoner/NotifikasjonerPage";
import { TiltaksgjennomforingInfo } from "./pages/tiltaksgjennomforinger/TiltaksgjennomforingInfo";
import { TiltaksgjennomforingPage } from "./pages/tiltaksgjennomforinger/TiltaksgjennomforingPage";
import { TiltaksgjennomforingerPage } from "./pages/tiltaksgjennomforinger/TiltaksgjennomforingerPage";
import { DetaljerTiltakstypePage } from "./pages/tiltakstyper/DetaljerTiltakstypePage";
import { TiltakstypeInfo } from "./pages/tiltakstyper/TiltakstypeInfo";
import { TiltakstyperPage } from "./pages/tiltakstyper/TiltakstyperPage";
import { AvtalerForTiltakstype } from "./pages/tiltakstyper/avtaler/AvtalerForTiltakstype";
import { avtaleFilter } from "./api/atoms";
import { useAvtaler } from "./api/avtaler/useAvtaler";

if (import.meta.env.PROD) {
  initializeFaro({
    url: import.meta.env.VITE_FARO_URL || "http://localhost:12347/collect",
    app: {
      name: "mr-admin-flate",
    },
  });
}

export function App() {
  const { data: ansatt, isLoading: ansattIsLoading, error } = useHentAnsatt();
  const { data: avtaler } = useAvtaler();

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
        rolle === NavAnsattRolle.BETABRUKER || rolle === NavAnsattRolle.TEAM_MULIGHETSROMMET,
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
        <Route path="avtaler" element={<AvtalerForTiltakstype />} errorElement={<ErrorPage />} />
      </Route>
      <Route path="avtaler" element={<AvtalerPage />} errorElement={<ErrorPage />}>
        <Route
          index
          element={
            <>
              <Avtalefilter avtalefilter={avtaleFilter} />
              <AvtaleTabell paginerteAvtaler={avtaler} avtalefilter={avtaleFilter} />
            </>
          }
        />
        <Route
          path="utkast"
          element={<UtkastListe utkastType={Utkast.type.AVTALE} />}
          errorElement={<ErrorPage />}
        />
      </Route>
      <Route path="avtaler/:avtaleId" element={<DetaljerAvtalePage />} errorElement={<ErrorPage />}>
        <Route index element={<AvtaleInfo />} errorElement={<ErrorPage />} />
        <Route path="notater" element={<NotaterAvtalePage />} errorElement={<ErrorPage />} />
        <Route
          path="tiltaksgjennomforinger"
          element={<TiltaksgjennomforingerForAvtale />}
          errorElement={<ErrorPage />}
        >
          <Route
            index
            element={
              <>
                <Tiltaksgjennomforingfilter
                  skjulFilter={{
                    tiltakstype: true,
                  }}
                />
                <TiltaksgjennomforingsTabell
                  skjulKolonner={{
                    tiltakstype: true,
                    arrangor: true,
                  }}
                />
              </>
            }
            errorElement={<ErrorPage />}
          />
          <Route
            path="utkast"
            element={<UtkastListe utkastType={Utkast.type.TILTAKSGJENNOMFORING} />}
            errorElement={<ErrorPage />}
          />
        </Route>
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
        <Route
          path="notater"
          element={<NotaterTiltaksgjennomforingerPage />}
          errorElement={<ErrorPage />}
        />
        <Route path="deltakere" element={<DeltakerListe />} errorElement={<ErrorPage />} />
      </Route>
      <Route
        path="tiltaksgjennomforinger/:tiltaksgjennomforingId"
        element={<TiltaksgjennomforingPage />}
        errorElement={<ErrorPage />}
      >
        <Route index element={<TiltaksgjennomforingInfo />} errorElement={<ErrorPage />} />
        <Route
          path="notater"
          element={<NotaterTiltaksgjennomforingerPage />}
          errorElement={<ErrorPage />}
        />
        <Route path="deltakere" element={<DeltakerListe />} errorElement={<ErrorPage />} />
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
