import { BodyShort, Box, Heading } from "@navikt/ds-react";
import { useRouteError } from "react-router";

export function ErrorPage() {
  const error = useRouteError() as
    | {
        statusText?: string;
        message?: string;
      }
    | undefined;

  return (
    <>
      <Box id="error-page" className="prose w-1/2 m-auto mt-5">
        <Heading size="large">Oops!</Heading>
        <BodyShort>Her har det skjedd en feil 🥺</BodyShort>
        <BodyShort>
          Feilmelding: <i>{error?.statusText || error?.message}</i>
        </BodyShort>
      </Box>
    </>
  );
}
