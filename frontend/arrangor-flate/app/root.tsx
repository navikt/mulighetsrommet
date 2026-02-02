import {
  isRouteErrorResponse,
  Links,
  LoaderFunction,
  Meta,
  MetaFunction,
  Outlet,
  Scripts,
  ScrollRestoration,
  useLoaderData,
  useLocation,
  useNavigate,
  useRouteError,
} from "react-router";
import parse from "html-react-parser";
import { ReactNode, useEffect } from "react";
import { DekoratorElements, fetchSsrDekorator } from "~/services/dekorator/dekorator.server";
import useInjectDecoratorScript from "~/services/dekorator/useInjectScript";
import "./tailwind.css";
import { ErrorPage } from "./components/common/ErrorPage";
import { isDemo } from "./services/environment";
import { Alert, Box, Heading, Link, Page } from "@navikt/ds-react";
import { Header } from "./components/header/Header";
import { initializeLogs, pushError } from "~/faro";

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

  useEffect(() => {
    initializeLogs();
  });

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
        <script type="module">import nais from "/nais.js"; window.nais = nais;</script>
        <Meta />
        <Links />
        <script
          dangerouslySetInnerHTML={{
            __html: `window.isDemo = ${isDemo()}`,
          }}
        />
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
          <Scripts />
        </Page>
      </Box>
    </html>
  );
}

function DekoratorHeader({ dekorator }: { dekorator?: DekoratorElements }) {
  if (isDemo()) {
    return (
      <Alert fullWidth variant="warning">
        <Heading spacing size="small" level="3">
          Demo Arrangørflate
        </Heading>
        Denne demoen er ment for NAV-ansatte som vil ha et overblikk av hvilke muligheter
        tiltaksarrangører har i våre flater.
        <br />
        Applikasjonsansvarlige:{" "}
        <Link href="https://teamkatalog.nav.no/team/aa730c95-b437-497b-b1ae-0ccf69a10997">
          Team Valp
        </Link>
        <br />
        <b>OBS!</b> Demoen inneholder ikke ekte data og kan til tider være ustabil.
      </Alert>
    );
  }
  if (dekorator) {
    return parse(dekorator.header);
  }
  return null;
}

export const ErrorBoundary = () => {
  const navigate = useNavigate();
  const error = useRouteError();

  useEffect(() => {
    if (isRouteErrorResponse(error)) {
      if (error.status === 401) {
        navigate(`/oauth2/login?redirect=${window.location.pathname}`);
      }
      if (error.status === 403) {
        navigate("/ingen-tilgang");
      }
    }
  }, [error, navigate]);

  useEffect(() => {
    pushError(error);
  }, [error]);

  if (isRouteErrorResponse(error)) {
    return (
      <Dokument>
        <ErrorPage
          heading={error.status === 404 ? "Siden ble ikke funnet" : `Feil ${error.status}`}
          body={[error.data.title, error.data.detail]}
          navigate={navigate}
        />
      </Dokument>
    );
  } else {
    let message: string | undefined;

    if (error instanceof Error) {
      message = error.message;
    } else if (typeof error === "string") {
      message = error;
    }

    return (
      <Dokument>
        <ErrorPage
          heading="Ojsann! Noe gikk galt"
          body={[
            "Det oppstod en uventet feil. Dette er ikke din feil, men vår.",
            "Vi jobber med å løse problemet. Vennligst prøv igjen senere.",
            "Feilmelding: " + (message ?? "N/A"),
          ]}
          navigate={navigate}
        />
      </Dokument>
    );
  }
};

export default App;
