import { Alert, BodyShort, VStack } from "@navikt/ds-react";
import { EyeSlashIcon } from "@navikt/aksel-icons";
import { ErrorBoundary, FallbackProps } from "react-error-boundary";
import {
  getProblemDetailTraceId,
  isProblemDetail,
} from "@mr/frontend-common/components/error-handling/errors";
import { PropsWithChildren } from "react";

interface Props {
  detail: string;
  traceId?: string;
}

export function ManglerTilgangTilPersonAlert({ detail, traceId }: Props) {
  return (
    <Alert variant="warning" role="alert">
      <VStack gap="space-4">
        <BodyShort>
          <EyeSlashIcon title="Mangler tilgang" fontSize="1.25rem" aria-hidden /> {detail}
        </BodyShort>
        {traceId && <BodyShort>Trace ID: {traceId}</BodyShort>}
      </VStack>
    </Alert>
  );
}

function isManglerTilgangTilPersonError(
  error: unknown,
): error is { type: "mangler-tilgang-til-person"; detail: string; traceId?: unknown } {
  return isProblemDetail(error) && error.type === "mangler-tilgang-til-person";
}

function ManglerTilgangTilPersonFallback({ error }: FallbackProps) {
  if (isManglerTilgangTilPersonError(error)) {
    return (
      <ManglerTilgangTilPersonAlert
        detail={error.detail}
        traceId={getProblemDetailTraceId(error)}
      />
    );
  }
  throw error;
}

export function ManglerTilgangTilPersonErrorBoundary({
  children,
  resetKeys,
}: PropsWithChildren<{ resetKeys?: unknown[] }>) {
  return (
    <ErrorBoundary fallbackRender={ManglerTilgangTilPersonFallback} resetKeys={resetKeys}>
      {children}
    </ErrorBoundary>
  );
}
