import { Arrangor, ArrangorflateService } from "api-client";
import { BodyShort, Box, Heading } from "@navikt/ds-react";
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

export const meta: MetaFunction = () => [{ title: "Utbetalinger" }];

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
        navigate("/ingen-tilgang"); // Use navigate inside useEffect
      }
    }
  }, [error, navigate]);

  if (isRouteErrorResponse(error)) {
    return (
      <Dokument arrangorer={[]}>
        <Heading spacing size="large" level="2">
          {error.status}
        </Heading>
        <Box background="bg-default" padding={"10"}>
          <BodyShort>{error.data.title}</BodyShort>
          <BodyShort>{error.data.detail}</BodyShort>
        </Box>
      </Dokument>
    );
  } else {
    return (
      <Dokument arrangorer={[]}>
        <Heading spacing size="large" level="2">
          Ojsann!
        </Heading>
        <Box background="bg-default" padding={"10"}>
          <BodyShort>Det skjedde en uventet feil.</BodyShort>
          <BodyShort>Vennligst prøv igjen senere</BodyShort>
        </Box>
      </Dokument>
    );
  }
};

export default App;
