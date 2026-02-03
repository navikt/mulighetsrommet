import {
  isRouteErrorResponse,
  Links,
  LoaderFunction,
  Meta,
  MetaFunction,
  Outlet,
  redirect,
  Scripts,
  ScrollRestoration,
  useLoaderData,
  useLocation,
} from "react-router";
import parse from "html-react-parser";
import { ReactNode } from "react";
import { DekoratorElements, fetchSsrDekorator } from "~/services/dekorator/dekorator.server";
import useInjectDecoratorScript from "~/services/dekorator/useInjectScript";
import "./tailwind.css";
import { ErrorPage, ErrorPageNotFound } from "./components/common/ErrorPage";
import { isDemo } from "./services/environment";
import { Box, GlobalAlert, Link, Page } from "@navikt/ds-react";
import { Header } from "./components/header/Header";
import { pushError } from "~/faro";
import { Route } from "./+types/root";

export const meta: MetaFunction = () => [{ title: "Utbetalinger til tiltaksarrangør" }];

export const loader: LoaderFunction = async () => {
  let dekorator = null;
  if (process.env.DISABLE_DEKORATOR !== "true") {
    dekorator = await fetchSsrDekorator();
  }
  return {
    dekorator,
  };
};

export type LoaderData = {
  dekorator: DekoratorElements | null;
};

function App() {
  const { dekorator } = useLoaderData<LoaderData>();

  return (
    <Dokument dekorator={dekorator || undefined}>
      <Outlet />
    </Dokument>
  );
}

function Dokument({ dekorator, children }: { dekorator?: DekoratorElements; children: ReactNode }) {
  useInjectDecoratorScript(dekorator?.scripts);
  const location = useLocation();
  const isLandingPage = location.pathname === "/";

  return (
    <html lang="no">
      <head>
        <meta charSet="utf-8" />
        <meta name="viewport" content="width=device-width, initial-scale=1" />
        <Meta />
        <Links />
        {dekorator && parse(dekorator.head)}
      </head>
      <Box asChild background={isLandingPage ? "default" : "sunken"}>
        <Page as="body" footer={dekorator && parse(dekorator.footer)}>
          <DekoratorHeader dekorator={dekorator} />
          <Header />
          <Page.Block as="main" width="2xl" gutters>
            {children}
          </Page.Block>
          <ScrollRestoration />
          <script
            dangerouslySetInnerHTML={{
              __html: `window.isDemo = ${isDemo()}`,
            }}
          />
          <script type="module">import nais from "/nais.js"; window.nais = nais;</script>
          <Scripts />
        </Page>
      </Box>
    </html>
  );
}

function DekoratorHeader({ dekorator }: { dekorator?: DekoratorElements }) {
  if (isDemo()) {
    return (
      <GlobalAlert status="warning">
        <GlobalAlert.Header>
          <GlobalAlert.Title as="h3">Demo Arrangørflate</GlobalAlert.Title>
        </GlobalAlert.Header>
        <GlobalAlert.Content>
          Denne demoen er ment for NAV-ansatte som vil ha et overblikk av hvilke muligheter
          tiltaksarrangører har i våre flater.
          <br />
          Applikasjonsansvarlige:{" "}
          <Link href="https://teamkatalog.nav.no/team/aa730c95-b437-497b-b1ae-0ccf69a10997">
            Team Valp
          </Link>
          <br />
          <b>OBS!</b> Demoen inneholder ikke ekte data og kan til tider være ustabil.
        </GlobalAlert.Content>
      </GlobalAlert>
    );
  }
  if (dekorator) {
    return parse(dekorator.header);
  }
  return null;
}

export function ErrorBoundary({ error }: Route.ErrorBoundaryProps) {
  if (isRouteErrorResponse(error)) {
    if (error.status === 401) {
      const redirectPath = typeof window !== "undefined" ? window.location.pathname : "/";
      throw redirect(`/oauth2/login?redirect=${redirectPath}`);
    }
    if (error.status === 403) {
      throw redirect("/ingen-tilgang");
    }
    if (error.status === 404) {
      return (
        <Dokument>
          <ErrorPageNotFound errorText={error.data?.detail} />
        </Dokument>
      );
    }
    if (error.status === 200)
      return (
        <Dokument>
          <ErrorPage
            title="Ukjent feil"
            statusCode={500}
            errorText="En teknisk feil på våre servere gjør at siden er utilgjengelig. Dette skyldes ikke noe du gjorde."
          />
        </Dokument>
      );
    else {
      pushError(error);
      return (
        <Dokument>
          <ErrorPage
            statusCode={error.status}
            title={error.data?.title}
            errorText={error.data?.detail}
          />
        </Dokument>
      );
    }
  } else {
    pushError(error);
    return (
      <Dokument>
        <ErrorPage
          title="Ukjent feil"
          statusCode={500}
          errorText={
            "Det oppstod en uventet feil. Dette er ikke din feil, men vår. " +
            "Vi jobber med å løse problemet. Vennligst prøv igjen senere. " +
            (error instanceof Error ? `Feilmelding: ${error.message}` : "")
          }
        />
      </Dokument>
    );
  }
}

export default App;
