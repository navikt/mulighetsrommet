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
import css from "./root.module.css";
import { ErrorPage } from "./components/ErrorPage";
import { problemDetailResponse } from "./utils/validering";

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
  };
};

export type LoaderData = {
  dekorator: DekoratorElements | null;
  arrangortilganger: Arrangor[];
};

function App() {
  const { dekorator, arrangortilganger } = useLoaderData<LoaderData>();

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
  arrangorer: Arrangor[];
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
        {dekorator && parse(dekorator.header)}
        <Header arrangorer={arrangorer} />
        <main className={css.main}>{children}</main>
        <ScrollRestoration />
        <Scripts />
        {dekorator && parse(dekorator.footer)}
      </body>
    </html>
  );
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
    return (
      <Dokument arrangorer={[]}>
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
