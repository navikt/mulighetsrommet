import { ArbeidsbenkPage } from "@/pages/arbeidsbenk/ArbeidsbenkPage";
import { arbeidsbenkLoader } from "@/pages/arbeidsbenk/arbeidsbenkLoader";
import { OppgaverPage } from "@/pages/arbeidsbenk/oppgaver/OppgaverPage";
import { oppgaverLoader } from "@/pages/arbeidsbenk/oppgaver/oppgaverLoader";
import { DeltakerlisteContainer } from "@/pages/gjennomforing/deltakerliste/DeltakerlisteContainer";
import { TilsagnForGjennomforingContainer } from "@/pages/gjennomforing/tilsagn/tabell/TilsagnForGjennomforingContainer";
import { getWebInstrumentations, initializeFaro } from "@grafana/faro-web-sdk";
import { AnsattService, NavAnsatt, NavAnsattRolle } from "@mr/api-client-v2";
import { Page } from "@navikt/ds-react";
import { QueryClient } from "@tanstack/react-query";
import { createBrowserRouter, Outlet, RouterProvider, useLoaderData } from "react-router";
import { Forside } from "./Forside";
import IkkeAutentisertApp from "./IkkeAutentisertApp";
import { IngenLesetilgang } from "./IngenLesetilgang";
import { AdministratorHeader } from "./components/administrator/AdministratorHeader";
import { Notifikasjonsliste } from "./components/notifikasjoner/Notifikasjonsliste";
import { initializeAmplitude } from "./logging/amplitude";
import { ErrorPage } from "./pages/ErrorPage";
import { NotifikasjonerPage } from "./pages/arbeidsbenk/notifikasjoner/NotifikasjonerPage";
import { notifikasjonLoader } from "./pages/arbeidsbenk/notifikasjoner/notifikasjonerLoader";
import { ArrangorPage } from "./pages/arrangor/ArrangorPage";
import { ArrangorerPage } from "./pages/arrangor/ArrangorerPage";
import { AvtaleFormPage } from "./pages/avtaler/AvtaleFormPage";
import { AvtaleInfo } from "./pages/avtaler/AvtaleInfo";
import { AvtalePage } from "./pages/avtaler/AvtalePage";
import { AvtalerPage } from "./pages/avtaler/AvtalerPage";
import { avtaleLoader, avtaleSkjemaLoader } from "./pages/avtaler/avtaleLoader";
import { GjennomforingFormPage } from "./pages/gjennomforing/GjennomforingFormPage";
import { GjennomforingInfo } from "./pages/gjennomforing/GjennomforingInfo";
import { GjennomforingPage } from "./pages/gjennomforing/GjennomforingPage";
import { GjennomforingerForAvtalePage } from "./pages/gjennomforing/GjennomforingerForAvtalePage";
import { GjennomforingerPage } from "./pages/gjennomforing/GjennomforingerPage";
import {
  gjennomforingFormLoader,
  gjennomforingLoader,
} from "./pages/gjennomforing/gjennomforingLoaders";
import { TilsagnDetaljer } from "./pages/gjennomforing/tilsagn/detaljer/TilsagnDetaljer";
import { tilsagnDetaljerLoader } from "./pages/gjennomforing/tilsagn/detaljer/tilsagnDetaljerLoader";
import { OpprettTilsagnFormPage } from "./pages/gjennomforing/tilsagn/opprett/OpprettTilsagnFormPage";
import { opprettTilsagnLoader } from "./pages/gjennomforing/tilsagn/opprett/opprettTilsagnLoader";
import { RedigerTilsagnFormPage } from "./pages/gjennomforing/tilsagn/rediger/RedigerTilsagnFormPage";
import { redigerTilsagnLoader } from "./pages/gjennomforing/tilsagn/rediger/redigerTilsagnLoader";
import { tilsagnForGjennomforingLoader } from "./pages/gjennomforing/tilsagn/tabell/tilsagnForGjennomforingLoader";
import { OpprettUtbetalingPage } from "./pages/gjennomforing/utbetaling/OpprettUtbetalingPage";
import { UtbetalingPage } from "./pages/gjennomforing/utbetaling/UtbetalingPage";
import { UtbetalingerForGjennomforingContainer } from "./pages/gjennomforing/utbetaling/UtbetalingerForGjennomforingContainer";
import { utbetalingPageLoader } from "./pages/gjennomforing/utbetaling/utbetalingPageLoader";
import { utbetalingerForGjennomforingLoader } from "./pages/gjennomforing/utbetaling/utbetalingerForGjennomforingLoader";
import { DetaljerTiltakstypePage } from "./pages/tiltakstyper/DetaljerTiltakstypePage";
import { TiltakstypeInfo } from "./pages/tiltakstyper/TiltakstypeInfo";
import { TiltakstyperPage } from "./pages/tiltakstyper/TiltakstyperPage";
import { AvtalerForTiltakstypePage } from "./pages/tiltakstyper/avtaler/AvtalerForTiltakstypePage";
import { tiltakstypeLoader, tiltakstyperLoader } from "./pages/tiltakstyper/tiltakstypeLoaders";

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

const ansattQuery = {
  queryKey: ["ansatt", "info"],
  queryFn: async () => {
    const { data } = await AnsattService.hentInfoOmAnsatt();
    return data;
  },
};

const ansattLoader = (queryClient: QueryClient) => async () => {
  return queryClient.ensureQueryData(ansattQuery);
};

const router = () => {
  const queryClient = new QueryClient();

  return createBrowserRouter(
    [
      {
        path: "/",
        element: <App />,
        errorElement: <ErrorPage />,
        loader: ansattLoader(queryClient),
        children: [
          {
            path: "tiltakstyper",
            element: <TiltakstyperPage />,
            errorElement: <ErrorPage />,
            loader: tiltakstyperLoader(queryClient),
          },
          {
            path: "tiltakstyper/:tiltakstypeId",
            element: <DetaljerTiltakstypePage />,
            errorElement: <ErrorPage />,
            loader: tiltakstypeLoader(queryClient),
            children: [
              {
                index: true,
                element: <TiltakstypeInfo />,
                errorElement: <ErrorPage />,
                loader: tiltakstypeLoader(queryClient),
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
            element: <AvtaleFormPage />,
            errorElement: <ErrorPage />,
            loader: avtaleSkjemaLoader,
          },
          {
            path: "avtaler/skjema",
            element: <AvtaleFormPage />,
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
            path: "gjennomforinger/:gjennomforingId/utbetalinger",
            element: <GjennomforingPage />,
            errorElement: <ErrorPage />,
            loader: gjennomforingLoader,
            children: [
              {
                index: true,
                element: <UtbetalingerForGjennomforingContainer />,
                errorElement: <ErrorPage />,
                loader: utbetalingerForGjennomforingLoader,
              },
            ],
          },
          {
            path: "gjennomforinger/:gjennomforingId/utbetalinger/skjema",
            element: <GjennomforingPage />,
            errorElement: <ErrorPage />,
            loader: gjennomforingLoader,
            children: [
              {
                index: true,
                element: <OpprettUtbetalingPage />,
                errorElement: <ErrorPage />,
                loader: utbetalingerForGjennomforingLoader,
              },
            ],
          },
          {
            path: "gjennomforinger/:gjennomforingId/deltakerliste",
            element: <GjennomforingPage />,
            errorElement: <ErrorPage />,
            loader: gjennomforingLoader,
            children: [
              {
                index: true,
                element: <DeltakerlisteContainer />,
                errorElement: <ErrorPage />,
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
            element: <OpprettTilsagnFormPage />,
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
            element: <RedigerTilsagnFormPage />,
            errorElement: <ErrorPage />,
            loader: redigerTilsagnLoader,
          },
          {
            path: "gjennomforinger/:gjennomforingId/utbetalinger/:utbetalingId",
            element: <UtbetalingPage />,
            errorElement: <ErrorPage />,
            loader: utbetalingPageLoader,
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
};

export function AppWithRouter() {
  return <RouterProvider router={router()} />;
}
