import { BodyShort, Box, Heading, InternalHeader, Page } from "@navikt/ds-react";
import { Link, useRouteError } from "react-router";
import { PORTEN_URL } from "../constants";

export function ErrorPage() {
  const error = useRouteError() as
    | {
        statusText?: string;
        message?: string;
      }
    | undefined;

  return (
    <Page>
      <Page.Block as="header">
        <InternalHeader>
          <InternalHeader.Title as="h1">
            <Link className="no-underline text-white" to="/">
              Nav Tiltaksadministrasjon
            </Link>
          </InternalHeader.Title>
        </InternalHeader>
      </Page.Block>
      <Box id="error-page" className="prose w-1/2 m-auto mt-5 p-4 rounded-md">
        <img className="rounded-md size-56" src="sorry-puppy.webp" alt="En valp som er lei seg" />
        <Heading size="large">Oops!</Heading>
        <BodyShort>Her har det skjedd en feil!</BodyShort>
        <BodyShort>
          Feilmelding: <i>{error?.statusText || error?.message || "N/A"}</i>
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
