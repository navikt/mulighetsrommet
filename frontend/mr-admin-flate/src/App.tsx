import { ArbeidsbenkPage } from "@/pages/arbeidsbenk/ArbeidsbenkPage";
import { OppgaverPage } from "@/pages/arbeidsbenk/oppgaver/OppgaverPage";
import { DeltakerlisteContainer } from "@/pages/gjennomforing/deltakerliste/DeltakerlisteContainer";
import { TilsagnForGjennomforingContainer } from "@/pages/gjennomforing/tilsagn/tabell/TilsagnForGjennomforingContainer";
import { getWebInstrumentations, initializeFaro } from "@grafana/faro-web-sdk";
import { AnsattService, NavAnsattRolle } from "@mr/api-client-v2";
import { useApiQuery } from "@mr/frontend-common";
import { Page } from "@navikt/ds-react";
import { QueryClient, useQueryClient } from "@tanstack/react-query";
import { createBrowserRouter, Outlet, RouterProvider } from "react-router";
import { Forside } from "./Forside";
import IkkeAutentisertApp from "./IkkeAutentisertApp";
import { IngenLesetilgang } from "./IngenLesetilgang";
import { QueryKeys } from "./api/QueryKeys";
import { lagreFilterAction } from "./api/lagret-filter/lagretFilterAction";
import { AdministratorHeader } from "./components/administrator/AdministratorHeader";
import { Notifikasjonsliste } from "./components/notifikasjoner/Notifikasjonsliste";
import { initializeAmplitude } from "./logging/amplitude";
import { ErrorPage } from "./pages/ErrorPage";
import { NotifikasjonerPage } from "./pages/arbeidsbenk/notifikasjoner/NotifikasjonerPage";
import { setLestStatusForNotifikasjonAction } from "./pages/arbeidsbenk/notifikasjoner/notifikasjonerAction";
import { ArrangorPage } from "./pages/arrangor/ArrangorPage";
import { ArrangorerPage } from "./pages/arrangor/ArrangorerPage";
import { AvtaleFormPage } from "./pages/avtaler/AvtaleFormPage";
import { AvtaleInfo } from "./pages/avtaler/AvtaleInfo";
import { AvtalePage } from "./pages/avtaler/AvtalePage";
import { AvtalerPage } from "./pages/avtaler/AvtalerPage";
import { GjennomforingFormPage } from "./pages/gjennomforing/GjennomforingFormPage";
import { GjennomforingInfo } from "./pages/gjennomforing/GjennomforingInfo";
import { GjennomforingPage } from "./pages/gjennomforing/GjennomforingPage";
import { GjennomforingerForAvtalePage } from "./pages/gjennomforing/GjennomforingerForAvtalePage";
import { GjennomforingerPage } from "./pages/gjennomforing/GjennomforingerPage";
import { publiserAction } from "./pages/gjennomforing/gjennomforingActions";
import { TilsagnDetaljer } from "./pages/gjennomforing/tilsagn/detaljer/TilsagnDetaljer";
import { OpprettTilsagnFormPage } from "./pages/gjennomforing/tilsagn/opprett/OpprettTilsagnFormPage";
import { RedigerTilsagnFormPage } from "./pages/gjennomforing/tilsagn/rediger/RedigerTilsagnFormPage";
import { OpprettUtbetalingPage } from "./pages/gjennomforing/utbetaling/OpprettUtbetalingPage";
import { UtbetalingerForGjennomforingContainer } from "./pages/gjennomforing/utbetaling/UtbetalingerForGjennomforingContainer";
import { DetaljerTiltakstypePage } from "./pages/tiltakstyper/DetaljerTiltakstypePage";
import { TiltakstypeInfo } from "./pages/tiltakstyper/TiltakstypeInfo";
import { TiltakstyperPage } from "./pages/tiltakstyper/TiltakstyperPage";
import { AvtalerForTiltakstypePage } from "./pages/tiltakstyper/avtaler/AvtalerForTiltakstypePage";
import { Suspense } from "react";
import { Laster } from "./components/laster/Laster";
import { InlineErrorBoundary } from "./ErrorBoundary";
import { UtbetalingPage } from "./pages/gjennomforing/utbetaling/UtbetalingPage";

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
  const { data: ansatt } = useApiQuery(ansattQuery);
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
        <Suspense fallback={<Laster tekst="Laster..." />}>
          <InlineErrorBoundary>
            <Outlet />
          </InlineErrorBoundary>
        </Suspense>
      </Page.Block>
    </Page>
  );
}

const ansattQuery = {
  queryKey: QueryKeys.ansatt(),
  queryFn: async () => {
    return await AnsattService.hentInfoOmAnsatt();
  },
};

const router = (queryClient: QueryClient) => {
  return createBrowserRouter(
    [
      {
        path: "/",
        element: <App />,
        errorElement: <ErrorPage />,
        children: [
          {
            path: "tiltakstyper",
            element: <TiltakstyperPage />,
            errorElement: <ErrorPage />,
          },
          {
            path: "tiltakstyper/:tiltakstypeId",
            element: <DetaljerTiltakstypePage />,
            errorElement: <ErrorPage />,
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
            action: lagreFilterAction(queryClient),
          },
          {
            path: "avtaler/:avtaleId",
            element: <AvtalePage />,
            errorElement: <ErrorPage />,
            children: [
              {
                index: true,
                element: <AvtaleInfo />,
                errorElement: <ErrorPage />,
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
          },
          {
            path: "avtaler/skjema",
            element: <AvtaleFormPage />,
            errorElement: <ErrorPage />,
          },
          {
            path: "avtaler/:avtaleId/gjennomforinger/skjema",
            element: <GjennomforingFormPage />,
            errorElement: <ErrorPage />,
          },
          {
            path: "gjennomforinger/skjema",
            element: <GjennomforingFormPage />,
            errorElement: <ErrorPage />,
          },
          {
            path: "gjennomforinger/",
            element: <GjennomforingerPage />,
            errorElement: <ErrorPage />,
            action: lagreFilterAction(queryClient),
          },
          {
            path: "gjennomforinger/:gjennomforingId",
            element: <GjennomforingPage />,
            errorElement: <ErrorPage />,
            action: publiserAction(queryClient),
            children: [
              {
                index: true,
                element: <GjennomforingInfo />,
                errorElement: <ErrorPage />,
              },
            ],
          },
          {
            path: "gjennomforinger/:gjennomforingId/tilsagn",
            element: <GjennomforingPage />,
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
            path: "gjennomforinger/:gjennomforingId/utbetalinger",
            element: <GjennomforingPage />,
            errorElement: <ErrorPage />,
            children: [
              {
                index: true,
                element: <UtbetalingerForGjennomforingContainer />,
                errorElement: <ErrorPage />,
              },
            ],
          },
          {
            path: "gjennomforinger/:gjennomforingId/utbetalinger/skjema",
            element: <GjennomforingPage />,
            errorElement: <ErrorPage />,
            children: [
              {
                index: true,
                element: <OpprettUtbetalingPage />,
                errorElement: <ErrorPage />,
              },
            ],
          },
          {
            path: "gjennomforinger/:gjennomforingId/deltakerliste",
            element: <GjennomforingPage />,
            errorElement: <ErrorPage />,
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
          },
          {
            path: "gjennomforinger/:gjennomforingId/tilsagn/opprett-tilsagn",
            element: <OpprettTilsagnFormPage />,
            errorElement: <ErrorPage />,
          },
          {
            path: "gjennomforinger/:gjennomforingId/tilsagn/:tilsagnId",
            element: <TilsagnDetaljer />,
            errorElement: <ErrorPage />,
          },
          {
            path: "gjennomforinger/:gjennomforingId/tilsagn/:tilsagnId/rediger-tilsagn",
            element: <RedigerTilsagnFormPage />,
            errorElement: <ErrorPage />,
          },
          {
            path: "gjennomforinger/:gjennomforingId/utbetalinger/:utbetalingId",
            element: <UtbetalingPage />,
            errorElement: <ErrorPage />,
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
            children: [
              {
                path: "notifikasjoner",
                element: <NotifikasjonerPage />,
                action: setLestStatusForNotifikasjonAction(queryClient),
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
  const queryClient = useQueryClient();
  return <RouterProvider router={router(queryClient)} />;
}
