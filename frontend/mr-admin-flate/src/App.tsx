import { OppgaveoversiktPage } from "@/pages/oppgaveoversikt/OppgaveoversiktPage";
import { OppgaverPage } from "@/pages/oppgaveoversikt/oppgaver/OppgaverPage";
import { DeltakerlisteContainer } from "@/pages/gjennomforing/deltakerliste/DeltakerlisteContainer";
import { TilsagnForGjennomforingPage } from "@/pages/gjennomforing/tilsagn/TilsagnForGjennomforingPage";
import { getWebInstrumentations, initializeFaro } from "@grafana/faro-web-sdk";
import { Page } from "@navikt/ds-react";
import { createBrowserRouter, Outlet, RouteObject, RouterProvider } from "react-router";
import { ForsidePage } from "./pages/forside/ForsidePage";
import { AdministratorHeader } from "./components/administrator/AdministratorHeader";
import { NotifikasjonerList } from "./components/notifikasjoner/NotifikasjonerList";
import { ErrorPage } from "./pages/ErrorPage";
import { NotifikasjonerPage } from "./pages/oppgaveoversikt/notifikasjoner/NotifikasjonerPage";
import { ArrangorPage } from "./pages/arrangor/ArrangorPage";
import { ArrangorerPage } from "./pages/arrangor/ArrangorerPage";
import { AvtalePage } from "./pages/avtaler/AvtalePage";
import { AvtalerPage } from "./pages/avtaler/AvtalerPage";
import { GjennomforingFormPage } from "./pages/gjennomforing/GjennomforingFormPage";
import { GjennomforingPage } from "./pages/gjennomforing/GjennomforingPage";
import { GjennomforingerPage } from "./pages/gjennomforing/GjennomforingerPage";
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
import { NewAvtaleFormPage } from "./pages/avtaler/NewAvtaleFormPage";
import { NewGjennomforingFormPage } from "./pages/gjennomforing/NewGjennomforingFormPage";
import { TilsagnPage } from "./pages/gjennomforing/tilsagn/detaljer/TilsagnPage";
import { GjennomforingDetaljer } from "./pages/gjennomforing/GjennomforingDetaljer";
import { RedaksjoneltInnholdGjennomforing } from "./components/redaksjoneltInnhold/RedaksjoneltInnholdGjennomforing";

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

export function App() {
  const router = createBrowserRouter(routes, { basename });
  return <RouterProvider router={router} />;
}

function AppLayout() {
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

const routes: RouteObject[] = [
  {
    path: "/",
    element: <AppLayout />,
    errorElement: <ErrorPage />,
    children: [
      {
        index: true,
        element: <ForsidePage />,
      },
      {
        path: "error",
        element: <ErrorPage />,
        errorElement: <ErrorPage />,
      },
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
          },
          {
            path: "avtaler",
            element: <AvtalerForTiltakstypePage />,
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
      },
      {
        path: "avtaler/skjema",
        element: <NewAvtaleFormPage />,
        errorElement: <ErrorPage />,
      },
      {
        path: "avtaler/:avtaleId/skjema",
        element: <AvtalePage />,
        errorElement: <ErrorPage />,
      },
      {
        path: "avtaler/:avtaleId/gjennomforinger/skjema",
        element: <NewGjennomforingFormPage />,
        errorElement: <ErrorPage />,
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
        children: [
          {
            index: true,
            element: <GjennomforingDetaljer />,
          },
          {
            path: "redaksjonelt-innhold",
            element: <RedaksjoneltInnholdGjennomforing />,
          },
          {
            path: "deltakerliste/*",
            element: <DeltakerlisteContainer />,
          },
          {
            path: "tilsagn",
            element: <TilsagnForGjennomforingPage />,
          },
          {
            path: "utbetalinger",
            element: <UtbetalingerForGjennomforingContainer />,
          },
          {
            path: "utbetalinger/skjema",
            element: <OpprettUtbetalingPage />,
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
        element: <TilsagnPage />,
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
        path: "oppgaveoversikt",
        element: <OppgaveoversiktPage />,
        errorElement: <ErrorPage />,
        children: [
          {
            path: "notifikasjoner",
            element: <NotifikasjonerPage />,
            children: [
              {
                index: true,
                element: <NotifikasjonerList lest={false} />,
              },
              {
                path: "tidligere",
                element: <NotifikasjonerList lest={true} />,
              },
            ],
          },
          {
            path: "oppgaver",
            element: <OppgaverPage />,
            children: [
              {
                index: true,
                element: <NotifikasjonerList lest={false} />,
              },
              {
                path: "fullforte",
                element: <NotifikasjonerList lest={true} />,
              },
            ],
          },
        ],
      },
    ],
  },
];
