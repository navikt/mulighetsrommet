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
import { ArrangorerPage } from "./pages/arrangor/ArrangorerPage";
import { AvtaleInfo } from "./pages/avtaler/AvtaleInfo";
import { AvtalePage } from "./pages/avtaler/AvtalePage";
import { AvtaleSkjemaPage } from "./pages/avtaler/AvtaleSkjemaPage";
import { AvtalerPage } from "./pages/avtaler/AvtalerPage";
import { DetaljerTiltakstypePage } from "./pages/tiltakstyper/DetaljerTiltakstypePage";
import { TiltakstypeInfo } from "./pages/tiltakstyper/TiltakstypeInfo";
import { TiltakstyperPage } from "./pages/tiltakstyper/TiltakstyperPage";
import { AvtalerForTiltakstypePage } from "./pages/tiltakstyper/avtaler/AvtalerForTiltakstypePage";
import { tiltakstypeLoader, tiltakstyperLoaders } from "./pages/tiltakstyper/tiltakstyperLoaders";
import { ArbeidsbenkPage } from "@/pages/arbeidsbenk/ArbeidsbenkPage";
import { OppgaverPage } from "@/pages/arbeidsbenk/oppgaver/OppgaverPage";
import { arbeidsbenkLoader } from "@/pages/arbeidsbenk/arbeidsbenkLoader";
import { avtaleLoader, avtaleSkjemaLoader } from "./pages/avtaler/avtaleLoader";
import { oppgaverLoader } from "@/pages/arbeidsbenk/oppgaver/oppgaverLoader";
import { Page } from "@navikt/ds-react";
import { ArrangorPage } from "./pages/arrangor/ArrangorPage";
import { GjennomforingFormPage } from "./pages/gjennomforing/GjennomforingFormPage";
import { GjennomforingInfo } from "./pages/gjennomforing/GjennomforingInfo";
import { GjennomforingPage } from "./pages/gjennomforing/GjennomforingPage";
import { GjennomforingerForAvtalePage } from "./pages/gjennomforing/GjennomforingerForAvtalePage";
import { GjennomforingerPage } from "./pages/gjennomforing/GjennomforingerPage";
import {
  gjennomforingFormLoader,
  gjennomforingLoader,
} from "./pages/gjennomforing/gjennomforingLoaders";
import { RefusjonskravForGjennomforingContainer } from "./pages/gjennomforing/refusjonskrav/RefusjonskravForGjennomforingContainer";
import { RefusjonskravDetaljer } from "./pages/gjennomforing/refusjonskrav/detaljer/RefusjonskravDetaljer";
import { refusjonskravDetaljerLoader } from "./pages/gjennomforing/refusjonskrav/detaljer/refusjonskravDetaljerLoader";
import { refusjonskravForGjennomforingLoader } from "./pages/gjennomforing/refusjonskrav/refusjonskravForGjennomforingLoader";
import { tilsagnDetaljerLoader } from "./pages/gjennomforing/tilsagn/detaljer/tilsagnDetaljerLoader";
import { opprettTilsagnLoader } from "./pages/gjennomforing/tilsagn/opprett/opprettTilsagnLoader";
import { RedigerTilsagnSkjemaPage } from "./pages/gjennomforing/tilsagn/rediger/RedigerTilsagnSkjemaPage";
import { redigerTilsagnLoader } from "./pages/gjennomforing/tilsagn/rediger/redigerTilsagnLoader";
import { tilsagnForGjennomforingLoader } from "./pages/gjennomforing/tilsagn/tabell/tilsagnForGjennomforingLoader";
import { OpprettTilsagnSkjemaPage } from "./pages/gjennomforing/tilsagn/opprett/OpprettTilsagnSkjemaPage";
import { TilsagnDetaljer } from "./pages/gjennomforing/tilsagn/detaljer/TilsagnDetaljer";
import { NotifikasjonerPage } from "./pages/arbeidsbenk/notifikasjoner/NotifikasjonerPage";
import { notifikasjonLoader } from "./pages/arbeidsbenk/notifikasjoner/notifikasjonerLoader";
import { TilsagnForGjennomforingContainer } from "@/pages/gjennomforing/tilsagn/tabell/TilsagnForGjennomforingContainer";

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
      <Page.Block as="header" className="max-w-[1920px]">
        <AdministratorHeader />
      </Page.Block>
      <Page.Block as="main" className="max-w-[1920px]">
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
                path: "gjennomforinger",
                element: <GjennomforingerForAvtalePage />,
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
            path: "avtaler/:avtaleId/gjennomforinger/skjema",
            element: <GjennomforingFormPage />,
            errorElement: <ErrorPage />,
            loader: gjennomforingFormLoader,
          },
          {
            path: "gjennomforinger/skjema",
            element: <GjennomforingFormPage />,
            errorElement: <ErrorPage />,
            loader: gjennomforingFormLoader,
          },
          {
            path: "gjennomforinger/",
            element: <GjennomforingerPage />,
            errorElement: <ErrorPage />,
          },
          {
            path: "gjennomforinger/:gjennomforingId",
            element: <GjennomforingPage />,
            errorElement: <ErrorPage />,
            loader: gjennomforingLoader,
            children: [
              {
                index: true,
                element: <GjennomforingInfo />,
                errorElement: <ErrorPage />,
                loader: gjennomforingLoader,
              },
            ],
          },
          {
            path: "gjennomforinger/:gjennomforingId/tilsagn",
            element: <GjennomforingPage />,
            errorElement: <ErrorPage />,
            loader: gjennomforingLoader,
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
            path: "gjennomforinger/:gjennomforingId/refusjonskrav",
            element: <GjennomforingPage />,
            errorElement: <ErrorPage />,
            loader: gjennomforingLoader,
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
            path: "gjennomforinger/:gjennomforingId/skjema",
            element: <GjennomforingFormPage />,
            errorElement: <ErrorPage />,
            loader: gjennomforingFormLoader,
          },
          {
            path: "gjennomforinger/:gjennomforingId/tilsagn/opprett-tilsagn",
            element: <OpprettTilsagnSkjemaPage />,
            errorElement: <ErrorPage />,
            loader: opprettTilsagnLoader,
          },
          {
            path: "gjennomforinger/:gjennomforingId/tilsagn/:tilsagnId",
            element: <TilsagnDetaljer />,
            errorElement: <ErrorPage />,
            loader: tilsagnDetaljerLoader,
          },
          {
            path: "gjennomforinger/:gjennomforingId/tilsagn/:tilsagnId/rediger-tilsagn",
            element: <RedigerTilsagnSkjemaPage />,
            errorElement: <ErrorPage />,
            loader: redigerTilsagnLoader,
          },
          {
            path: "gjennomforinger/:gjennomforingId/refusjonskrav/:refusjonskravId",
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
            element: <ArrangorPage />,
            errorElement: <ErrorPage />,
          },
          {
            path: "arbeidsbenk",
            element: <ArbeidsbenkPage />,
            errorElement: <ErrorPage />,
            loader: arbeidsbenkLoader,
            //element: <Notifikasjonsliste lest={false} />,
            children: [
              {
                path: "notifikasjoner",
                element: <NotifikasjonerPage />,
                loader: notifikasjonLoader,
                errorElement: <ErrorPage />,
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
                path: "oppgaver",
                element: <OppgaverPage />,
                loader: oppgaverLoader,
                errorElement: <ErrorPage />,
                children: [
                  {
                    index: true,
                    element: <Notifikasjonsliste lest={false} />,
                    errorElement: <ErrorPage />,
                  },
                  {
                    path: "fullforte",
                    element: <Notifikasjonsliste lest={true} />,
                    errorElement: <ErrorPage />,
                  },
                ],
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
