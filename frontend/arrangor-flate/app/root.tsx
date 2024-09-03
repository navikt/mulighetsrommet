import { BodyShort, Box, Heading } from "@navikt/ds-react";
import { LoaderFunction } from "@remix-run/node";
import {
  isRouteErrorResponse,
  json,
  Links,
  Meta,
  MetaFunction,
  Outlet,
  redirect,
  Scripts,
  ScrollRestoration,
  useLoaderData,
  useRouteError,
} from "@remix-run/react";
import parse from "html-react-parser";
import { ReactNode, useEffect } from "react";
import css from "./root.module.css";
import { Dekoratørfragmenter, hentSsrDekoratør } from "./services/dekoratør/dekorator.server";
import "./tailwind.css";
import { hentMiljø, Miljø } from "./services/miljø";
import useInjectDecoratorScript from "./services/dekoratør/useInjectScript";
import { Header } from "./components/Header";
import { RefusjonskravIkon } from "./components/icons/RefusjonskravIkon";

export const meta: MetaFunction = () => [{ title: "Refusjoner" }];

export const loader: LoaderFunction = async ({ request, context }) => {
  const miljø = hentMiljø();
  if (miljø !== Miljø.Lokalt) {
    if (!context.erAutorisert) {
      return redirect(`/oauth2/login?redirect=${request.url}`);
    }
  }

  return json({
    dekorator: await hentSsrDekoratør(),
  });
};

export type LoaderData = {
  dekorator: Dekoratørfragmenter | null;
};

function App() {
  const { dekorator } = useLoaderData<LoaderData>();

  return (
    <Dokument dekorator={dekorator || undefined}>
      <Outlet />
    </Dokument>
  );
}

function Dokument({
  dekorator,
  children,
}: {
  dekorator?: Dekoratørfragmenter;
  children: ReactNode;
}) {
  useInjectDecoratorScript(dekorator?.scripts);
  return (
    <html lang="en">
      <head>
        <meta charSet="utf-8" />
        <meta name="viewport" content="width=device-width, initial-scale=1" />
        <Meta />
        <Links />
        {dekorator && parse(dekorator.styles)}
      </head>
      <body>
        {dekorator && parse(dekorator.header)}
        <Header />
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
      <Dokument>
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
      <Dokument>
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
