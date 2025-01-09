import { getWebInstrumentations, initializeFaro } from "@grafana/faro-web-sdk";
import { AnsattService, NavAnsatt, NavAnsattRolle } from "@mr/api-client";
import { createBrowserRouter, Outlet, RouterProvider, useLoaderData } from "react-router";
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
import { OpprettTilsagnSkjemaPage } from "./pages/tiltaksgjennomforinger/tilsagn/opprett/OpprettTilsagnSkjemaPage";
import { TilsagnDetaljer } from "./pages/tiltaksgjennomforinger/tilsagn/detaljer/TilsagnDetaljer";
import { TilsagnForGjennomforingContainer } from "./pages/tiltaksgjennomforinger/tilsagn/tabell/TilsagnForGjennomforingContainer";
import { DetaljerTiltakstypePage } from "./pages/tiltakstyper/DetaljerTiltakstypePage";
import { TiltakstypeInfo } from "./pages/tiltakstyper/TiltakstypeInfo";
import { TiltakstyperPage } from "./pages/tiltakstyper/TiltakstyperPage";
import { AvtalerForTiltakstypePage } from "./pages/tiltakstyper/avtaler/AvtalerForTiltakstypePage";
import { tiltakstypeLoader, tiltakstyperLoaders } from "./pages/tiltakstyper/tiltakstyperLoaders";
import { avtaleLoader, avtaleSkjemaLoader } from "./pages/avtaler/avtaleLoader";
import {
  tiltaksgjennomforingLoader,
  tiltaksgjennomforingSkjemaLoader,
} from "./pages/tiltaksgjennomforinger/tiltaksgjennomforingLoaders";
import { tilsagnDetaljerLoader } from "./pages/tiltaksgjennomforinger/tilsagn/detaljer/tilsagnDetaljerLoader";
import { redigerTilsagnLoader } from "@/pages/tiltaksgjennomforinger/tilsagn/rediger/redigerTilsagnLoader";
import { opprettTilsagnLoader } from "@/pages/tiltaksgjennomforinger/tilsagn/opprett/opprettTilsagnLoader";
import { RedigerTilsagnSkjemaPage } from "@/pages/tiltaksgjennomforinger/tilsagn/rediger/RedigerTilsagnSkjemaPage";
import { tilsagnForGjennomforingLoader } from "@/pages/tiltaksgjennomforinger/tilsagn/tabell/tilsagnForGjennomforingLoader";
import { RefusjonskravForGjennomforingContainer } from "./pages/tiltaksgjennomforinger/refusjonskrav/RefusjonskravForGjennomforingContainer";
import { refusjonskravForGjennomforingLoader } from "./pages/tiltaksgjennomforinger/refusjonskrav/refusjonskravForGjennomforingLoader";
import { RefusjonskravDetaljer } from "./pages/tiltaksgjennomforinger/refusjonskrav/detaljer/RefusjonskravDetaljer";
import { refusjonskravDetaljerLoader } from "./pages/tiltaksgjennomforinger/refusjonskrav/detaljer/refusjonskravDetaljerLoader";
import { Page } from "@navikt/ds-react";

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
    <Page background="bg-subtle">
      <Page.Block as="header" width="2xl">
        <AdministratorHeader />
      </Page.Block>
      <Page.Block as="main" width="2xl">
        <Outlet />
      </Page.Block>
    </Page>
  );
}

async function ansattLoader() {
  return AnsattService.hentInfoOmAnsatt();
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
                loader: tiltakstypeLoader,
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
            loader: avtaleSkjemaLoader,
          },
          {
            path: "avtaler/skjema",
            element: <AvtaleSkjemaPage />,
            errorElement: <ErrorPage />,
            loader: avtaleSkjemaLoader,
          },
          {
            path: "tiltaksgjennomforinger/skjema",
            element: <TiltaksgjennomforingSkjemaPage />,
            errorElement: <ErrorPage />,
            loader: tiltaksgjennomforingSkjemaLoader,
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
            loader: tiltaksgjennomforingLoader,
            children: [
              {
                index: true,
                element: <TiltaksgjennomforingInfo />,
                errorElement: <ErrorPage />,
                loader: tiltaksgjennomforingLoader,
              },
            ],
          },
          {
            path: "tiltaksgjennomforinger/:tiltaksgjennomforingId",
            element: <TiltaksgjennomforingPage />,
            errorElement: <ErrorPage />,
            loader: tiltaksgjennomforingLoader,
            children: [
              {
                index: true,
                element: <TiltaksgjennomforingInfo />,
                errorElement: <ErrorPage />,
                loader: tiltaksgjennomforingLoader,
              },
            ],
          },
          {
            path: "tiltaksgjennomforinger/:tiltaksgjennomforingId/tilsagn",
            element: <TiltaksgjennomforingPage />,
            errorElement: <ErrorPage />,
            loader: tiltaksgjennomforingLoader,
            children: [
              {
                index: true,
                element: <TilsagnForGjennomforingContainer />,
                loader: tilsagnForGjennomforingLoader,
                errorElement: <ErrorPage />,
              },
            ],
          },
          {
            path: "tiltaksgjennomforinger/:tiltaksgjennomforingId/refusjonskrav",
            element: <TiltaksgjennomforingPage />,
            errorElement: <ErrorPage />,
            loader: tiltaksgjennomforingLoader,
            children: [
              {
                index: true,
                element: <RefusjonskravForGjennomforingContainer />,
                errorElement: <ErrorPage />,
                loader: refusjonskravForGjennomforingLoader,
              },
            ],
          },
          {
            path: "avtaler/:avtaleId/tiltaksgjennomforinger/:tiltaksgjennomforingId/skjema",
            element: <TiltaksgjennomforingSkjemaPage />,
            errorElement: <ErrorPage />,
            loader: tiltaksgjennomforingSkjemaLoader,
          },
          {
            path: "avtaler/:avtaleId/tiltaksgjennomforinger/:tiltaksgjennomforingId/tilsagn/opprett-tilsagn",
            element: <OpprettTilsagnSkjemaPage />,
            errorElement: <ErrorPage />,
            loader: opprettTilsagnLoader,
          },
          {
            path: "avtaler/:avtaleId/tiltaksgjennomforinger/:tiltaksgjennomforingId/tilsagn/:tilsagnId",
            element: <TilsagnDetaljer />,
            errorElement: <ErrorPage />,
            loader: tilsagnDetaljerLoader,
          },
          {
            path: "avtaler/:avtaleId/tiltaksgjennomforinger/:tiltaksgjennomforingId/tilsagn/:tilsagnId/rediger-tilsagn",
            element: <RedigerTilsagnSkjemaPage />,
            errorElement: <ErrorPage />,
            loader: redigerTilsagnLoader,
          },
          {
            path: "avtaler/:avtaleId/tiltaksgjennomforinger/:tiltaksgjennomforingId/refusjonskrav/:refusjonskravId",
            element: <RefusjonskravDetaljer />,
            errorElement: <ErrorPage />,
            loader: refusjonskravDetaljerLoader,
          },
          {
            path: "avtaler/:avtaleId/tiltaksgjennomforinger/skjema",
            element: <TiltaksgjennomforingSkjemaPage />,
            errorElement: <ErrorPage />,
            loader: tiltaksgjennomforingSkjemaLoader,
          },
          {
            path: "tiltaksgjennomforinger/:tiltaksgjennomforingId/skjema",
            element: <TiltaksgjennomforingSkjemaPage />,
            errorElement: <ErrorPage />,
            loader: tiltaksgjennomforingSkjemaLoader,
          },
          {
            path: "tiltaksgjennomforinger/:tiltaksgjennomforingId/tilsagn/opprett-tilsagn",
            element: <OpprettTilsagnSkjemaPage />,
            errorElement: <ErrorPage />,
            loader: opprettTilsagnLoader,
          },
          {
            path: "tiltaksgjennomforinger/:tiltaksgjennomforingId/tilsagn/:tilsagnId",
            element: <TilsagnDetaljer />,
            errorElement: <ErrorPage />,
            loader: tilsagnDetaljerLoader,
          },
          {
            path: "tiltaksgjennomforinger/:tiltaksgjennomforingId/tilsagn/:tilsagnId/rediger-tilsagn",
            element: <RedigerTilsagnSkjemaPage />,
            errorElement: <ErrorPage />,
            loader: redigerTilsagnLoader,
          },
          {
            path: "tiltaksgjennomforinger/:tiltaksgjennomforingId/refusjonskrav/:refusjonskravId",
            element: <RefusjonskravDetaljer />,
            errorElement: <ErrorPage />,
            loader: refusjonskravDetaljerLoader,
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
    },
  );

export function AppWithRouter() {
  return <RouterProvider router={router()} />;
}
