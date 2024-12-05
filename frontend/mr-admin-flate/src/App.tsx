import { getWebInstrumentations, initializeFaro } from "@grafana/faro-web-sdk";
import { AnsattService, NavAnsatt, NavAnsattRolle } from "@mr/api-client";
import { createBrowserRouter, Outlet, RouterProvider, useLoaderData } from "react-router-dom";
import { Forside } from "./Forside";
import IkkeAutentisertApp from "./IkkeAutentisertApp";
import { IngenLesetilgang } from "./IngenLesetilgang";
import { AdministratorHeader } from "./components/administrator/AdministratorHeader";
import { Notifikasjonsliste } from "./components/notifikasjoner/Notifikasjonsliste";
import { initializeAmplitude } from "./logging/amplitude";
import { ErrorPage } from "./pages/ErrorPage";
import { ArrangorPageContainer } from "./pages/arrangor/ArrangorPageContainer";
import { ArrangorerPage } from "./pages/arrangor/ArrangorerPage";
import { AvtaleInfo } from "./pages/avtaler/AvtaleInfo";
import { AvtalePage } from "./pages/avtaler/AvtalePage";
import { AvtaleSkjemaPage } from "./pages/avtaler/AvtaleSkjemaPage";
import { AvtalerPage } from "./pages/avtaler/AvtalerPage";
import { NotifikasjonerPage } from "./pages/notifikasjoner/NotifikasjonerPage";
import { notifikasjonLoader } from "./pages/notifikasjoner/notifikasjonerLoader";
import { TiltaksgjennomforingInfo } from "./pages/tiltaksgjennomforinger/TiltaksgjennomforingInfo";
import { TiltaksgjennomforingPage } from "./pages/tiltaksgjennomforinger/TiltaksgjennomforingPage";
import { TiltaksgjennomforingSkjemaPage } from "./pages/tiltaksgjennomforinger/TiltaksgjennomforingSkjemaPage";
import { TiltaksgjennomforingerForAvtalePage } from "./pages/tiltaksgjennomforinger/TiltaksgjennomforingerForAvtalePage";
import { TiltaksgjennomforingerPage } from "./pages/tiltaksgjennomforinger/TiltaksgjennomforingerPage";
import { OpprettTilsagnSkjemaPage } from "./pages/tiltaksgjennomforinger/tilsagn/OpprettTilsagnSkjemaPage";
import { TilsagnDetaljer } from "./pages/tiltaksgjennomforinger/tilsagn/TilsagnDetaljer";
import { TilsagnForGjennomforingContainer } from "./pages/tiltaksgjennomforinger/tilsagn/TilsagnForGjennomforingContainer";
import { DetaljerTiltakstypePage } from "./pages/tiltakstyper/DetaljerTiltakstypePage";
import { TiltakstypeInfo } from "./pages/tiltakstyper/TiltakstypeInfo";
import { TiltakstyperPage } from "./pages/tiltakstyper/TiltakstyperPage";
import { AvtalerForTiltakstypePage } from "./pages/tiltakstyper/avtaler/AvtalerForTiltakstypePage";
import { tiltakstypeLoader, tiltakstyperLoaders } from "./pages/tiltakstyper/tiltakstyperLoaders";
import { avtaleLoader } from "./pages/avtaler/avtaleLoader";

const basename = import.meta.env.BASE_URL;

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
  const ansatt = useLoaderData() as NavAnsatt;
  if (!ansatt) {
    return null;
  }

  if (!ansatt.roller.includes(NavAnsattRolle.TILTAKADMINISTRASJON_GENERELL)) {
    return <IngenLesetilgang />;
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
    <>
      <AdministratorHeader />
      <Outlet />
    </>
  );
}

async function ansattLoader() {
  const data = await AnsattService.hentInfoOmAnsatt();
  return data;
}

const router = () =>
  createBrowserRouter(
    [
      {
        path: "/",
        element: <App />,
        errorElement: <ErrorPage />,
        loader: ansattLoader,
        children: [
          {
            path: "tiltakstyper",
            element: <TiltakstyperPage />,
            errorElement: <ErrorPage />,
            loader: tiltakstyperLoaders,
          },
          {
            path: "tiltakstyper/:tiltakstypeId",
            element: <DetaljerTiltakstypePage />,
            errorElement: <ErrorPage />,
            loader: tiltakstypeLoader,
            children: [
              {
                index: true,
                element: <TiltakstypeInfo />,
                errorElement: <ErrorPage />,
              },
              {
                path: "avtaler",
                element: <AvtalerForTiltakstypePage />,
                errorElement: <ErrorPage />,
              },
            ],
          },
          {
            path: "avtaler",
            element: <AvtalerPage />,
            errorElement: <ErrorPage />,
          },
          {
            path: "avtaler/:avtaleId",
            element: <AvtalePage />,
            errorElement: <ErrorPage />,
            loader: avtaleLoader,
            children: [
              {
                index: true,
                element: <AvtaleInfo />,
                errorElement: <ErrorPage />,
                loader: avtaleLoader,
              },
              {
                path: "tiltaksgjennomforinger",
                element: <TiltaksgjennomforingerForAvtalePage />,
                errorElement: <ErrorPage />,
              },
            ],
          },
          {
            path: "avtaler/:avtaleId/skjema",
            element: <AvtaleSkjemaPage />,
            errorElement: <ErrorPage />,
          },
          {
            path: "avtaler/skjema",
            element: <AvtaleSkjemaPage />,
            errorElement: <ErrorPage />,
          },
          {
            path: "tiltaksgjennomforinger/skjema",
            element: <TiltaksgjennomforingSkjemaPage />,
            errorElement: <ErrorPage />,
          },
          {
            path: "tiltaksgjennomforinger/",
            element: <TiltaksgjennomforingerPage />,
            errorElement: <ErrorPage />,
          },
          {
            path: "avtaler/:avtaleId/tiltaksgjennomforinger/:tiltaksgjennomforingId",
            element: <TiltaksgjennomforingPage />,
            errorElement: <ErrorPage />,
            children: [
              {
                index: true,
                element: <TiltaksgjennomforingInfo />,
                errorElement: <ErrorPage />,
              },
            ],
          },
          {
            path: "tiltaksgjennomforinger/:tiltaksgjennomforingId",
            element: <TiltaksgjennomforingPage />,
            errorElement: <ErrorPage />,
            children: [
              {
                index: true,
                element: <TiltaksgjennomforingInfo />,
                errorElement: <ErrorPage />,
              },
            ],
          },
          {
            path: "tiltaksgjennomforinger/:tiltaksgjennomforingId/tilsagn",
            element: <TiltaksgjennomforingPage />,
            errorElement: <ErrorPage />,
            children: [
              {
                index: true,
                element: <TilsagnForGjennomforingContainer />,
                errorElement: <ErrorPage />,
              },
            ],
          },
          {
            path: "avtaler/:avtaleId/tiltaksgjennomforinger/:tiltaksgjennomforingId/skjema",
            element: <TiltaksgjennomforingSkjemaPage />,
            errorElement: <ErrorPage />,
          },
          {
            path: "avtaler/:avtaleId/tiltaksgjennomforinger/:tiltaksgjennomforingId/opprett-tilsagn",
            element: <OpprettTilsagnSkjemaPage />,
            errorElement: <ErrorPage />,
          },
          {
            path: "avtaler/:avtaleId/tiltaksgjennomforinger/:tiltaksgjennomforingId/tilsagn/:tilsagnId",
            element: <TilsagnDetaljer />,
            errorElement: <ErrorPage />,
          },
          {
            path: "avtaler/:avtaleId/tiltaksgjennomforinger/:tiltaksgjennomforingId/tilsagn/:tilsagnId/rediger-tilsagn",
            element: <OpprettTilsagnSkjemaPage />,
            errorElement: <ErrorPage />,
          },
          {
            path: "avtaler/:avtaleId/tiltaksgjennomforinger/skjema",
            element: <TiltaksgjennomforingSkjemaPage />,
            errorElement: <ErrorPage />,
          },
          {
            path: "tiltaksgjennomforinger/:tiltaksgjennomforingId/skjema",
            element: <TiltaksgjennomforingSkjemaPage />,
            errorElement: <ErrorPage />,
          },
          {
            path: "tiltaksgjennomforinger/:tiltaksgjennomforingId/opprett-tilsagn",
            element: <OpprettTilsagnSkjemaPage />,
            errorElement: <ErrorPage />,
          },
          {
            path: "tiltaksgjennomforinger/:tiltaksgjennomforingId/tilsagn/:tilsagnId",
            element: <TilsagnDetaljer />,
            errorElement: <ErrorPage />,
          },
          {
            path: "tiltaksgjennomforinger/:tiltaksgjennomforingId/tilsagn/:tilsagnId/rediger-tilsagn",
            element: <OpprettTilsagnSkjemaPage />,
            errorElement: <ErrorPage />,
          },
          {
            path: "arrangorer",
            element: <ArrangorerPage />,
            errorElement: <ErrorPage />,
          },
          {
            path: "arrangorer/:arrangorId",
            element: <ArrangorPageContainer />,
            errorElement: <ErrorPage />,
          },
          {
            path: "notifikasjoner",
            element: <NotifikasjonerPage />,
            errorElement: <ErrorPage />,
            loader: notifikasjonLoader,
            children: [
              {
                index: true,
                element: <Notifikasjonsliste lest={false} />,
                errorElement: <ErrorPage />,
              },
              {
                path: "tidligere",
                element: <Notifikasjonsliste lest={true} />,
                errorElement: <ErrorPage />,
              },
            ],
          },
          {
            index: true,
            element: <Forside />,
          },
        ],
      },
    ],
    {
      basename,
      future: {
        v7_fetcherPersist: true,
        v7_normalizeFormMethod: true,
        v7_partialHydration: true,
        v7_skipActionErrorRevalidation: true,
        v7_relativeSplatPath: true,
      },
    },
  );

export function AppWithRouter() {
  return <RouterProvider router={router()} />;
}
