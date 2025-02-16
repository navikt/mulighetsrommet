import { BodyShort, Box, Heading, Page } from "@navikt/ds-react";
import { Link, useRouteError } from "react-router";
import { PORTEN_URL } from "../constants";

export function ErrorPage() {
  const error = useRouteError() as
    | {
        statusText?: string;
        detail?: string;
        message?: string;
      }
    | undefined;

  return (
    <Page>
      <Box id="error-page" className="prose w-1/2 m-auto mt-5 p-4 rounded-md">
        <img className="rounded-md size-56" src="/sorry-puppy.webp" alt="En valp som er lei seg" />
        <Heading size="large">Oops!</Heading>
        <BodyShort>Her har det skjedd en feil!</BodyShort>
        <BodyShort>
          Feilmelding: <i>{error?.statusText || error?.message || error?.detail || "N/A"}</i>
        </BodyShort>
        <BodyShort>
          Dersom feilen vedvarer, vennligst ta kontakt med Team Valp via{" "}
          <Link target="_blank" rel="noopener noreferrer" to={PORTEN_URL}>
            PORTEN
          </Link>
        </BodyShort>
      </Box>
    </Page>
  );
}
