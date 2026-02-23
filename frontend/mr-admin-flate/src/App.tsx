import { OppgaveoversiktPage } from "@/pages/oppgaveoversikt/OppgaveoversiktPage";
import { OppgaverPage } from "@/pages/oppgaveoversikt/oppgaver/OppgaverPage";
import { DeltakerlisteContainer } from "@/pages/gjennomforing/deltakerliste/DeltakerlisteContainer";
import { TilsagnForGjennomforingPage } from "@/pages/gjennomforing/tilsagn/TilsagnForGjennomforingPage";
import { getWebInstrumentations, initializeFaro, InternalLoggerLevel } from "@grafana/faro-web-sdk";
import { Page, Theme } from "@navikt/ds-react";
import { createBrowserRouter, Outlet, RouteObject, RouterProvider } from "react-router";
import { ForsidePage } from "./pages/forside/ForsidePage";
import { AdministratorHeader } from "./components/administrator/AdministratorHeader";
import { NotifikasjonerList } from "./components/notifikasjoner/NotifikasjonerList";
import { ErrorPage } from "./pages/ErrorPage";
import { ArrangorPage } from "./pages/arrangor/ArrangorPage";
import { ArrangorerPage } from "./pages/arrangor/ArrangorerPage";
import { AvtalePage } from "./pages/avtaler/AvtalePage";
import { AvtalerPage } from "./pages/avtaler/AvtalerPage";
import { RedigerGjennomforingFormPage } from "./pages/gjennomforing/RedigerGjennomforingFormPage";
import { GjennomforingPage } from "./pages/gjennomforing/GjennomforingPage";
import { GjennomforingerPage } from "./pages/gjennomforing/GjennomforingerPage";
import { OpprettTilsagnFormPage } from "./pages/gjennomforing/tilsagn/opprett/OpprettTilsagnFormPage";
import { RedigerTilsagnFormPage } from "./pages/gjennomforing/tilsagn/rediger/RedigerTilsagnFormPage";
import { OpprettUtbetalingPage } from "./pages/gjennomforing/utbetaling/OpprettUtbetalingPage";
import { UtbetalingerForGjennomforingContainer } from "./pages/gjennomforing/utbetaling/UtbetalingerForGjennomforingContainer";
import { DetaljerTiltakstypePage } from "./pages/tiltakstyper/DetaljerTiltakstypePage";
import { TiltakstyperPage } from "./pages/tiltakstyper/TiltakstyperPage";
import { Suspense } from "react";
import { Laster } from "./components/laster/Laster";
import { InlineErrorBoundary } from "./ErrorBoundary";
import { UtbetalingPage } from "./pages/gjennomforing/utbetaling/UtbetalingPage";
import { OpprettAvtaleFormPage } from "./pages/avtaler/OpprettAvtaleFormPage";
import { OpprettGjennomforingFormPage } from "./pages/gjennomforing/OpprettGjennomforingFormPage";
import { TilsagnPage } from "./pages/gjennomforing/tilsagn/detaljer/TilsagnPage";
import { GjennomforingDetaljer } from "./pages/gjennomforing/GjennomforingDetaljer";
import { RedaksjoneltInnholdGjennomforing } from "./components/redaksjoneltInnhold/RedaksjoneltInnholdGjennomforing";
import { AvtaleDetaljer } from "./pages/avtaler/AvtaleDetaljer";
import { AvtalePersonvern } from "./pages/avtaler/AvtalePersonvern";
import { GjennomforingerForAvtalePage } from "./pages/gjennomforing/GjennomforingerForAvtalePage";
import { RedaksjoneltInnholdPreview } from "./components/redaksjoneltInnhold/RedaksjoneltInnholdPreview";
import { AvtaleFormPage } from "./pages/avtaler/AvtaleFormPage";
import { TilsagnDetaljer } from "./pages/gjennomforing/tilsagn/detaljer/TilsagnDetaljer";
import { InnsendingoversiktPage } from "./pages/innsendinger/InnsendingsoversiktPage";
import { UtdatertKlientBanner } from "./api/UtdatertKlientBanner";
import { createHead, UnheadProvider } from "@unhead/react/client";
import { Head } from "@unhead/react";
import { AvtaleDetaljerForm } from "./components/avtaler/AvtaleDetaljerForm";
import { AvtalePersonvernForm } from "./components/avtaler/AvtalePersonvernForm";
import { AvtaleInformasjonForVeiledereForm } from "./components/avtaler/AvtaleInformasjonForVeiledereForm";

const basename = import.meta.env.BASE_URL;

const head = createHead();

if (import.meta.env.VITE_FARO_URL) {
  initializeFaro({
    url: import.meta.env.VITE_FARO_URL,
    app: {
      name: "mr-admin-flate",
    },
    instrumentations: [...getWebInstrumentations({ captureConsole: true })],
    isolate: true,
    internalLoggerLevel: InternalLoggerLevel.OFF,
  });
}

export function App() {
  const router = createBrowserRouter(routes, { basename });
  return <RouterProvider router={router} />;
}

function AppLayout() {
  return (
    <UnheadProvider head={head}>
      <Head>
        <script
          defer
          src="https://cdn.nav.no/team-researchops/sporing/sporing.js"
          data-host-url="https://umami.nav.no"
          data-domains="tiltaksadministrasjon.intern.dev.nav.no"
          data-website-id="7b4a1f84-e34c-46d9-ae4a-de244d3c9ea9"
        />
        <script
          defer
          src="https://cdn.nav.no/team-researchops/sporing/sporing.js"
          data-host-url="https://umami.nav.no"
          data-domains="tiltaksadministrasjon.intern.nav.no, tiltaksadministrasjon.ansatt.nav.no"
          data-website-id="182ed73a-eaa9-4ea0-9e30-7a0a74c5c396"
        />
      </Head>
      <Theme theme="light" hasBackground={false}>
        <Page>
          <Page.Block as="header">
            <AdministratorHeader />
            <UtdatertKlientBanner />
          </Page.Block>
          <Page.Block as="main">
            <Suspense fallback={<Laster tekst="Laster..." />}>
              <InlineErrorBoundary>
                <Outlet />
              </InlineErrorBoundary>
            </Suspense>
          </Page.Block>
        </Page>
      </Theme>
    </UnheadProvider>
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
        children: [
          {
            index: true,
            element: <AvtaleDetaljer />,
          },
          {
            path: "personvern",
            element: <AvtalePersonvern />,
          },
          {
            path: "veilederinformasjon",
            element: <RedaksjoneltInnholdPreview />,
          },
          {
            path: "gjennomforinger",
            element: <GjennomforingerForAvtalePage />,
          },
        ],
      },
      {
        path: "avtaler/opprett-avtale",
        element: <OpprettAvtaleFormPage />,
        errorElement: <ErrorPage />,
      },
      {
        path: "avtaler/:avtaleId/skjema",
        element: <AvtaleFormPage />,
        errorElement: <ErrorPage />,
        children: [
          {
            index: true,
            element: <AvtaleDetaljerForm />,
            errorElement: <ErrorPage />,
          },
          {
            path: "personvern",
            element: <AvtalePersonvernForm />,
            errorElement: <ErrorPage />,
          },
          {
            path: "veilederinformasjon",
            element: <AvtaleInformasjonForVeiledereForm />,
            errorElement: <ErrorPage />,
          },
        ],
      },
      {
        path: "avtaler/:avtaleId/gjennomforinger/skjema",
        element: <OpprettGjennomforingFormPage />,
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
          { path: "tilsagn", element: <TilsagnForGjennomforingPage /> },
          {
            path: "utbetalinger",
            element: <UtbetalingerForGjennomforingContainer />,
          },
          {
            path: "utbetalinger/skjema",
            element: <OpprettUtbetalingPage />,
          },
          {
            path: "utbetalinger/opprett-utbetaling",
            element: <OpprettUtbetalingPage />,
          },
        ],
      },
      {
        path: "gjennomforinger/:gjennomforingId/tilsagn",
        element: <TilsagnPage />,
        errorElement: <ErrorPage />,
        children: [
          { path: "opprett-tilsagn", element: <OpprettTilsagnFormPage /> },
          { path: ":tilsagnId", element: <TilsagnDetaljer /> },
          { path: ":tilsagnId/rediger-tilsagn", element: <RedigerTilsagnFormPage /> },
        ],
      },
      {
        path: "gjennomforinger/:gjennomforingId/skjema",
        element: <RedigerGjennomforingFormPage />,
        errorElement: <ErrorPage />,
      },
      {
        path: "gjennomforinger/:gjennomforingId/redaksjonelt-innhold/skjema",
        element: <RedigerGjennomforingFormPage />,
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
            element: <NotifikasjonerList lest={false} />,
          },
          {
            path: "tidligere-notifikasjoner",
            element: <NotifikasjonerList lest={true} />,
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
      {
        path: "innsendingsoversikt",
        element: <InnsendingoversiktPage />,
        errorElement: <ErrorPage />,
      },
    ],
  },
];
