import { ArrangorflateArrangor, ArrangorflateService } from "api-client";
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
import { DekoratorElements, fetchSsrDekorator } from "~/services/dekorator/dekorator.server";
import useInjectDecoratorScript from "~/services/dekorator/useInjectScript";
import "./tailwind.css";
import { apiHeaders } from "./auth/auth.server";
import css from "./root.module.css";
import { ErrorPage } from "./components/common/ErrorPage";
import { problemDetailResponse } from "./utils/validering";
import { Header } from "./components/header/Header";
import { isDemo } from "./services/environment";
import { Alert, Heading, Link } from "@navikt/ds-react";
import { getFaro } from "./utils/telemetri";

export const meta: MetaFunction = () => [{ title: "Utbetalinger til tiltaksarrangør" }];

export const loader: LoaderFunction = async ({ request }) => {
  const { data: arrangortilganger, error } =
    await ArrangorflateService.getArrangorerInnloggetBrukerHarTilgangTil({
      headers: await apiHeaders(request),
    });

  if (!arrangortilganger) {
    throw problemDetailResponse(error);
  }

  let dekorator = null;
  if (process.env.DISABLE_DEKORATOR !== "true") {
    dekorator = await fetchSsrDekorator();
  }
  const telemetryUrl = process.env.TELEMETRY_URL;

  return {
    dekorator,
    arrangortilganger,
    telemetryUrl,
  };
};

export type LoaderData = {
  dekorator: DekoratorElements | null;
  arrangortilganger: ArrangorflateArrangor[];
  telemetryUrl: string;
};

function App() {
  const { dekorator, arrangortilganger, telemetryUrl } = useLoaderData<LoaderData>();

  useEffect(() => {
    getFaro(telemetryUrl);
  });

  return (
    <Dokument dekorator={dekorator || undefined} arrangorer={arrangortilganger}>
      <Outlet />
    </Dokument>
  );
}

function Dokument({
  dekorator,
  children,
  arrangorer,
}: {
  dekorator?: DekoratorElements;
  children: ReactNode;
  arrangorer: ArrangorflateArrangor[];
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
        <DekoratorHeader dekorator={dekorator} />
        <Header arrangorer={arrangorer} />
        <main id="maincontent" className={css.main}>
          {children}
        </main>
        <ScrollRestoration />
        <script
          dangerouslySetInnerHTML={{
            __html: `window.isDemo = ${isDemo()}`,
          }}
        />
        <Scripts />
        {dekorator && parse(dekorator.footer)}
      </body>
    </html>
  );
}

function DekoratorHeader({ dekorator }: { dekorator?: DekoratorElements }) {
  if (isDemo()) {
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
      <Dokument arrangorer={[]}>
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
      <Dokument arrangorer={[]}>
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
