import { useRouteError } from "react-router-dom";
import { BodyShort, Heading } from "@navikt/ds-react";

export function ErrorPage() {
  const error = useRouteError() as
    | {
        statusText?: string;
        message?: string;
      }
    | undefined;

  return (
    <div id="error-page">
      <Heading size="large">Oops!</Heading>
      <BodyShort>Her har det skjedd en feil 🥺</BodyShort>
      <BodyShort>
        <i>{error?.statusText || error?.message}</i>
      </BodyShort>
    </div>
  );
}
