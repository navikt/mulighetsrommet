import { LoaderFunction } from "@remix-run/node";
import {
  json,
  Links,
  Meta,
  MetaFunction,
  Outlet,
  redirect,
  Scripts,
  ScrollRestoration,
  useLoaderData,
} from "@remix-run/react";
import parse from "html-react-parser";
import { Dekoratørfragmenter, hentSsrDekoratør } from "./services/dekoratør/dekoratør.server";
import { hentMiljø, Miljø } from "./services/miljø";
import "./tailwind.css";
import { configureMock } from "./mocks";
import { ReactNode } from "react";
import css from "./root.module.css";

export const meta: MetaFunction = () => [{ title: "Refusjoner" }];

export const loader: LoaderFunction = async ({ request, context }) => {
  const miljø = hentMiljø();

  if (miljø === Miljø.Lokalt) {
    configureMock();
  } else {
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
        <main className={css.side}>{children}</main>
        <ScrollRestoration />
        <Scripts />
        {dekorator && parse(dekorator.footer)}
      </body>
    </html>
  );
}

export default App;
