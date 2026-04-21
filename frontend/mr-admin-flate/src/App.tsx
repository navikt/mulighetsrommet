import { OppgaveoversiktPage } from "@/pages/oppgaveoversikt/OppgaveoversiktPage";
import { OppgaverPage } from "@/pages/oppgaveoversikt/oppgaver/OppgaverPage";
import { DeltakerlisteContainer } from "@/pages/gjennomforing/deltakerliste/DeltakerlisteContainer";
import { TilsagnForGjennomforingPage } from "@/pages/gjennomforing/tilsagn/TilsagnForGjennomforingPage";
import { getWebInstrumentations, initializeFaro, InternalLoggerLevel } from "@grafana/faro-web-sdk";
import { Page, Theme } from "@navikt/ds-react";
import {
  createBrowserRouter,
  NonIndexRouteObject,
  Outlet,
  RouteObject,
  RouterProvider,
} from "react-router";
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
import { UtbetalingerForGjennomforingContainer } from "./pages/gjennomforing/utbetaling/UtbetalingerForGjennomforingContainer";
import { DetaljerTiltakstypePage } from "./pages/tiltakstyper/DetaljerTiltakstypePage";
import { TiltakstyperPage } from "./pages/tiltakstyper/TiltakstyperPage";
import { TiltakstypePage } from "./pages/tiltakstyper/TiltakstypePage";
import { TiltakstypeInformasjonForVeilederePage } from "./pages/tiltakstyper/TiltakstypeInformasjonForVeilederePage";
import { TiltakstypePageRedigerInformasjonForVeiledere } from "./pages/tiltakstyper/TiltakstypePageRedigerInformasjonForVeiledere";
import { TiltakstypeDeltakerRegistreringPage } from "./pages/tiltakstyper/TiltakstypeDeltakerRegistreringPage";
import { TiltakstypePageRedigerDeltakerRegistrering } from "./pages/tiltakstyper/TiltakstypePageRedigerDeltakerRegistrering";
import { Suspense } from "react";
import { Laster } from "./components/laster/Laster";
import { InlineErrorBoundary } from "./ErrorBoundary";
import { UtbetalingDetaljerPage } from "./pages/gjennomforing/utbetaling/UtbetalingDetaljerPage";
import { OpprettAvtaleFormPage } from "./pages/avtaler/OpprettAvtaleFormPage";
import { RedigerAvtaleDetaljerPage } from "./pages/avtaler/RedigerAvtaleDetaljerPage";
import { RedigerAvtalePersonvernPage } from "./pages/avtaler/RedigerAvtalePersonvernPage";
import { RedigerAvtaleVeilederinformasjonPage } from "./pages/avtaler/RedigerAvtaleVeilederinformasjonPage";
import { OpprettGjennomforingFormPage } from "./pages/gjennomforing/OpprettGjennomforingFormPage";
import { TilsagnPage } from "./pages/gjennomforing/tilsagn/detaljer/TilsagnPage";
import { GjennomforingDetaljer } from "./pages/gjennomforing/GjennomforingDetaljer";
import { GjennomforingRedaksjoneltInnhold } from "./pages/gjennomforing/GjennomforingRedaksjoneltInnhold";
import { AvtaleDetaljer } from "./pages/avtaler/AvtaleDetaljer";
import { AvtalePersonvern } from "./pages/avtaler/AvtalePersonvern";
import { GjennomforingerForAvtalePage } from "./pages/gjennomforing/GjennomforingerForAvtalePage";
import { TilsagnDetaljer } from "./pages/gjennomforing/tilsagn/detaljer/TilsagnDetaljer";
import { InnsendingoversiktPage } from "./pages/innsendinger/InnsendingsoversiktPage";
import { UtdatertKlientBanner } from "./api/UtdatertKlientBanner";
import { createHead, UnheadProvider } from "@unhead/react/client";
import { Head } from "@unhead/react";
import { OpprettUtbetalingPage } from "@/pages/gjennomforing/utbetaling/OpprettUtbetalingPage";
import { UtbetalingPage } from "@/pages/gjennomforing/utbetaling/UtbetalingPage";
import { RedigerUtbetalingPage } from "@/pages/gjennomforing/utbetaling/RedigerUtbetalingPage";
import { AvtaleRedaksjoneltInnhold } from "@/pages/avtaler/AvtaleRedaksjoneltInnhold";
import { TilskuddBehandlingDetaljerPage } from "./pages/tilskudd-behandling/TilskuddBehandlingDetaljerPage";
import { TilskuddBehandlingFormPage } from "./pages/tilskudd-behandling/TilskuddBehandlingFormPage";
import { TilskuddBehandlingerPage } from "./pages/tilskudd-behandling/TilskuddBehandlingerPage";

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
          <Page.Block as="header" className="max-w-384">
            <AdministratorHeader />
            <UtdatertKlientBanner />
          </Page.Block>
          <Page.Block as="main" className="max-w-384">
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

function route(config: Omit<NonIndexRouteObject, "errorElement">): NonIndexRouteObject {
  return { errorElement: <ErrorPage />, ...config };
}

const AVTALE_ROUTES: RouteObject[] = [
  { index: true, element: <AvtaleDetaljer /> },
  { path: "personvern", element: <AvtalePersonvern /> },
  { path: "veilederinformasjon", element: <AvtaleRedaksjoneltInnhold /> },
  { path: "gjennomforinger", element: <GjennomforingerForAvtalePage /> },
];

const GJENNOMFORING_ROUTES: RouteObject[] = [
  { index: true, element: <GjennomforingDetaljer /> },
  { path: "redaksjonelt-innhold", element: <GjennomforingRedaksjoneltInnhold /> },
  { path: "deltakerliste/*", element: <DeltakerlisteContainer /> },
  { path: "tilskudd-behandling", element: <TilskuddBehandlingerPage /> },
  { path: "tilsagn", element: <TilsagnForGjennomforingPage /> },
  { path: "utbetalinger", element: <UtbetalingerForGjennomforingContainer /> },
];

const TILSAGN_ROUTES: RouteObject[] = [
  { path: "opprett-tilsagn", element: <OpprettTilsagnFormPage /> },
  { path: ":tilsagnId", element: <TilsagnDetaljer /> },
  { path: ":tilsagnId/rediger-tilsagn", element: <RedigerTilsagnFormPage /> },
];

const UTBETALING_ROUTES: RouteObject[] = [
  { path: "opprett-utbetaling", element: <OpprettUtbetalingPage /> },
  { path: ":utbetalingId", element: <UtbetalingDetaljerPage /> },
  { path: ":utbetalingId/rediger-utbetaling", element: <RedigerUtbetalingPage /> },
];

const OPPGAVEOVERSIKT_ROUTES: RouteObject[] = [
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
];

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
      route({
        path: "error",
        element: <ErrorPage />,
      }),
      route({
        path: "tiltakstyper",
        element: <TiltakstyperPage />,
      }),
      route({
        path: "tiltakstyper/:tiltakstypeId",
        element: <TiltakstypePage />,
        children: [
          { index: true, element: <DetaljerTiltakstypePage /> },
          { path: "redaksjonelt-innhold", element: <TiltakstypeInformasjonForVeilederePage /> },
          { path: "deltaker-registrering", element: <TiltakstypeDeltakerRegistreringPage /> },
        ],
      }),
      route({
        path: "tiltakstyper/:tiltakstypeId/redaksjonelt-innhold/rediger",
        element: <TiltakstypePageRedigerInformasjonForVeiledere />,
      }),
      route({
        path: "tiltakstyper/:tiltakstypeId/deltaker-registrering/rediger",
        element: <TiltakstypePageRedigerDeltakerRegistrering />,
      }),
      route({
        path: "avtaler",
        element: <AvtalerPage />,
      }),
      route({ path: "avtaler/:avtaleId", element: <AvtalePage />, children: AVTALE_ROUTES }),
      route({ path: "avtaler/opprett", element: <OpprettAvtaleFormPage /> }),
      route({ path: "avtaler/:avtaleId/rediger", element: <RedigerAvtaleDetaljerPage /> }),
      route({
        path: "avtaler/:avtaleId/personvern/rediger",
        element: <RedigerAvtalePersonvernPage />,
      }),
      route({
        path: "avtaler/:avtaleId/veilederinformasjon/rediger",
        element: <RedigerAvtaleVeilederinformasjonPage />,
      }),
      route({
        path: "avtaler/:avtaleId/gjennomforinger/skjema",
        element: <OpprettGjennomforingFormPage />,
      }),
      route({ path: "gjennomforinger/", element: <GjennomforingerPage /> }),
      route({
        path: "gjennomforinger/:gjennomforingId",
        element: <GjennomforingPage />,
        children: GJENNOMFORING_ROUTES,
      }),
      route({
        path: "gjennomforinger/:gjennomforingId/tilsagn",
        element: <TilsagnPage />,
        children: TILSAGN_ROUTES,
      }),
      {
        path: "gjennomforinger/:gjennomforingId/tilskudd-behandling",
        element: <TilskuddBehandlingerPage />,
      },
      route({
        path: "gjennomforinger/:gjennomforingId/tilskudd-behandling/opprett",
        element: <TilskuddBehandlingFormPage />,
      }),
      route({
        path: "gjennomforinger/:gjennomforingId/tilskudd-behandling/:behandlingId",
        element: <TilskuddBehandlingDetaljerPage />,
      }),

      route({
        path: "gjennomforinger/:gjennomforingId/skjema",
        element: <RedigerGjennomforingFormPage />,
      }),
      route({
        path: "gjennomforinger/:gjennomforingId/redaksjonelt-innhold/skjema",
        element: <RedigerGjennomforingFormPage />,
      }),
      route({
        path: "gjennomforinger/:gjennomforingId/utbetalinger",
        element: <UtbetalingPage />,
        children: UTBETALING_ROUTES,
      }),
      route({ path: "arrangorer", element: <ArrangorerPage /> }),
      route({ path: "arrangorer/:arrangorId", element: <ArrangorPage /> }),
      route({
        path: "oppgaveoversikt",
        element: <OppgaveoversiktPage />,
        children: OPPGAVEOVERSIKT_ROUTES,
      }),
      route({ path: "innsendingsoversikt", element: <InnsendingoversiktPage /> }),
    ],
  },
];
