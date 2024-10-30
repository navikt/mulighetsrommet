import { Alert, BodyShort, Box, Heading } from "@navikt/ds-react";
import { LoaderFunction } from "@remix-run/node";
import {
  isRouteErrorResponse,
  json,
  Links,
  Meta,
  MetaFunction,
  Outlet,
  Scripts,
  ScrollRestoration,
  useLoaderData,
  useRouteError,
} from "@remix-run/react";
import parse from "html-react-parser";
import { ReactNode, useEffect } from "react";
import { checkValidToken } from "./auth/auth.server";
import { Header } from "./components/Header";
import css from "./root.module.css";
import { Dekoratørfragmenter, hentSsrDekoratør } from "./services/dekoratør/dekorator.server";
import useInjectDecoratorScript from "./services/dekoratør/useInjectScript";
import "./tailwind.css";
import { hentArrangortilgangerForBruker } from "./auth/arrangortilgang.server";

export const meta: MetaFunction = () => [{ title: "Refusjoner" }];

export const loader: LoaderFunction = async ({ request }) => {
  await checkValidToken(request);
  const arrangortilganger = await hentArrangortilgangerForBruker();

  return json({
    dekorator: await hentSsrDekoratør(),
    arrangortilganger,
  });
};

export type LoaderData = {
  dekorator: Dekoratørfragmenter | null;
  arrangortilganger: { navn: string; organisasjonsnummer: string }[]; // TODO Bytt til modell fra OpenAPI
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
  dekorator?: Dekoratørfragmenter;
  children: ReactNode;
  arrangorer: { navn: string; organisasjonsnummer: string }[]; // TODO Bytt til modell fra OpenAPI
}) {
  useInjectDecoratorScript(dekorator?.scripts);
  return (
    <html lang="en">
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
        <main className={css.side}>{children}</main>
        <ScrollRestoration />
        <Scripts />
        {dekorator && parse(dekorator.footer)}
      </body>
    </html>
  );
}

export const ErrorBoundary = () => {
  const error = useRouteError();

  useEffect(() => {
    if (isRouteErrorResponse(error) && error.status === 401) {
      redirectTilInnlogging();
    }
  });

  if (isRouteErrorResponse(error)) {
    return (
      <Dokument arrangorer={[]}>
        <Heading spacing size="large" level="2">
          {error.status}
        </Heading>
        <Box background="bg-default" padding={"10"}>
          <BodyShort>Det skjedde en uventet feil</BodyShort>
          <BodyShort>{error.data.message}</BodyShort>
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

const redirectTilInnlogging = () => {
  if (typeof window !== "undefined") {
    window.location.href = `/oauth2/login?redirect=${window.location.pathname}`;
  }
};

export default App;
