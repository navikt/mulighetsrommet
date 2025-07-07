import { Arrangor, ArrangorflateService } from "api-client";
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
  useNavigate,
  useRouteError,
} from "react-router";
import parse from "html-react-parser";
import { ReactNode, useEffect } from "react";
import { Header } from "./components/Header";
import { DekoratorElements, fetchSsrDekorator } from "~/services/dekorator/dekorator.server";
import useInjectDecoratorScript from "~/services/dekorator/useInjectScript";
import "./tailwind.css";
import { apiHeaders } from "./auth/auth.server";
import { problemDetailResponse } from "./utils";
import css from "./root.module.css";
import { ErrorPage } from "./components/ErrorPage";
import { Environment, getEnvironment } from "./services/environment";
import { Alert, Heading, Link } from "@navikt/ds-react";

export const meta: MetaFunction = () => [{ title: "Tilsagn og utbetalinger" }];

export const loader: LoaderFunction = async ({ request }) => {
  const { data: arrangortilganger, error } =
    await ArrangorflateService.getArrangorerInnloggetBrukerHarTilgangTil({
      headers: await apiHeaders(request),
    });

  if (!arrangortilganger || error) {
    throw problemDetailResponse(error);
  }

  let dekorator = null;
  if (process.env.DISABLE_DEKORATOR !== "true") {
    dekorator = await fetchSsrDekorator();
  }

  return {
    dekorator,
    arrangortilganger,
    env: getEnvironment(),
  };
};

export type LoaderData = {
  dekorator: DekoratorElements | null;
  arrangortilganger: Arrangor[];
  env: Environment;
};

function App() {
  const { dekorator, arrangortilganger, env } = useLoaderData<LoaderData>();

  return (
    <Dokument dekorator={dekorator || undefined} arrangorer={arrangortilganger} env={env}>
      <Outlet />
    </Dokument>
  );
}

function Dokument({
  dekorator,
  children,
  arrangorer,
  env,
}: {
  dekorator?: DekoratorElements;
  children: ReactNode;
  arrangorer: Arrangor[];
  env: Environment;
}) {
  useInjectDecoratorScript(dekorator?.scripts);
  return (
    <html lang="no">
      <head>
        <meta charSet="utf-8" />
        <meta name="viewport" content="width=device-width, initial-scale=1" />
        <Meta />
        <Links />
        {dekorator && parse(dekorator.head)}
      </head>
      <body>
        <DekoratorHeader dekorator={dekorator} env={env} />
        <Header arrangorer={arrangorer} />
        <main className={css.main}>{children}</main>
        <ScrollRestoration />
        <script
          dangerouslySetInnerHTML={{
            __html: `window.isDemo = ${env === Environment.Demo}`,
          }}
        />
        <Scripts />
        {dekorator && parse(dekorator.footer)}
      </body>
    </html>
  );
}

function DekoratorHeader({ dekorator, env }: { dekorator?: DekoratorElements; env: Environment }) {
  if (env === Environment.Demo) {
    return (
      <Alert fullWidth variant="warning" className="max-w-1920">
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
  const env = getEnvironment();

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

  if (isRouteErrorResponse(error)) {
    return (
      <Dokument arrangorer={[]} env={env}>
        <ErrorPage
          heading={error.status === 404 ? "Siden ble ikke funnet" : `Feil ${error.status}`}
          body={[error.data.title, error.data.detail]}
          navigate={navigate}
        />
      </Dokument>
    );
  } else {
    return (
      <Dokument arrangorer={[]} env={env}>
        <ErrorPage
          heading="Ojsann! Noe gikk galt"
          body={[
            "Det oppstod en uventet feil. Dette er ikke din feil, men vår.",
            "Vi jobber med å løse problemet. Vennligst prøv igjen senere.",
          ]}
          navigate={navigate}
        />
      </Dokument>
    );
  }
};

export default App;
