import { ErrorBoundary, FallbackProps } from "react-error-boundary";
import { Alert, BodyShort, Heading } from "@navikt/ds-react";
import { PropsWithChildren } from "react";
import { Link } from "react-router";
import { resolveErrorMessage } from "./errors";

interface DefaultErrorBoundaryProps extends PropsWithChildren {
  portenUrl: string;
}

export function ReloadAppErrorBoundary(props: DefaultErrorBoundaryProps) {
  return (
    <ErrorBoundary
      fallbackRender={(fallbackProps) => (
        <ReloadAppFallback portenUrl={props.portenUrl} {...fallbackProps} />
      )}
    >
      {props.children}
    </ErrorBoundary>
  );
}

export function InlineErrorBoundary(props: DefaultErrorBoundaryProps) {
  return (
    <ErrorBoundary
      fallbackRender={(fallbackProps) => (
        <InlineFallback portenUrl={props.portenUrl} {...fallbackProps} />
      )}
    >
      {props.children}
    </ErrorBoundary>
  );
}

interface DefaultErrorFallbackProps extends FallbackProps {
  portenUrl: string;
}

function InlineFallback({ error, portenUrl }: DefaultErrorFallbackProps) {
  const heading = resolveErrorMessage(error);

  return (
    <div className="error">
      <Alert variant="error">
        <Heading size="medium" level="2">
          {heading || "Det oppsto dessverre en feil"}
        </Heading>
        <BodyShort>
          Hvis problemet vedvarer opprett en sak via <a href={portenUrl}>Porten</a>.
        </BodyShort>
      </Alert>
    </div>
  );
}

function ReloadAppFallback({ error, portenUrl }: DefaultErrorFallbackProps) {
  const heading = resolveErrorMessage(error);

  return (
    <div className="error">
      <Alert variant="error">
        <Heading size="medium" level="2">
          {heading || "Det oppsto dessverre en feil"}
        </Heading>
        <BodyShort>
          Hvis problemet vedvarer opprett en sak via <a href={portenUrl}>Porten</a>.
        </BodyShort>
        <Link to="/" reloadDocument className="error-link">
          Ta meg til forsiden og prøv igjen
        </Link>
      </Alert>
    </div>
  );
}
