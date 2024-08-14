import { ErrorBoundary, FallbackProps } from "react-error-boundary";
import { ApiError } from "@mr/api-client";
import { Alert, BodyShort, Heading } from "@navikt/ds-react";
import { PropsWithChildren } from "react";
import { Link } from "react-router-dom";
import { PORTEN_URL } from "../../../mr-admin-flate/src/constants";
import { resolveErrorMessage } from "./errors";

export function ReloadAppErrorBoundary(props: PropsWithChildren) {
  return <ErrorBoundary FallbackComponent={ReloadAppFallback}>{props.children} </ErrorBoundary>;
}

export function InlineErrorBoundary(props: PropsWithChildren) {
  return <ErrorBoundary FallbackComponent={InlineFallback}>{props.children} </ErrorBoundary>;
}

export function InlineFallback({ error }: FallbackProps) {
  const heading = error instanceof ApiError ? resolveErrorMessage(error) : error.message;

  return (
    <div className="error">
      <Alert variant="error">
        <Heading size="medium" level="2">
          {heading || "Det oppsto dessverre en feil"}
        </Heading>
        <BodyShort>
          Hvis problemet vedvarer opprett en sak via <a href={PORTEN_URL}>Porten</a>.
        </BodyShort>
      </Alert>
    </div>
  );
}

export function ReloadAppFallback({ error }: FallbackProps) {
  const heading = error instanceof ApiError ? resolveErrorMessage(error) : error.message;

  return (
    <div className="error">
      <Alert variant="error">
        <Heading size="medium" level="2">
          {heading || "Det oppsto dessverre en feil"}
        </Heading>
        <BodyShort>
          Hvis problemet vedvarer opprett en sak via <a href={PORTEN_URL}>Porten</a>.
        </BodyShort>
        <Link to="/" reloadDocument className="error-link">
          Ta meg til forsiden og prøv igjen
        </Link>
      </Alert>
    </div>
  );
}
