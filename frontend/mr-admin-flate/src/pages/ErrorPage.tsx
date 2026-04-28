import { BodyShort, Box, Heading, Page, VStack } from "@navikt/ds-react";
import { Link, useLocation, useRouteError } from "react-router";
import { PORTEN_URL } from "@/constants";
import { IngenTilgang, NavAnsattManglerTilgangError } from "@/pages/IngenTilgang";
import { ProblemDetail } from "@tiltaksadministrasjon/api-client";
import { isProblemDetail } from "@mr/frontend-common/components/error-handling/errors";

type ErrorType = ProblemDetail | Error | string | null | undefined;

export function ErrorPage() {
  const routeError = useRouteError() as ErrorType;
  const location = useLocation() as {
    state: { problemDetail?: ProblemDetail; error?: Error } | null;
  };

  const error = routeError || location.state?.problemDetail || location.state?.error;
  if (isProblemDetail(error) && isNavAnsattManglerTilgangError(error)) {
    return <IngenTilgang error={error} />;
  }

  const title = getErrorTitle(error);
  const detail = getErrorDetail(error);
  return (
    <Page>
      <Box id="error-page" className="prose w-1/2 m-auto mt-5 p-4 rounded-md">
        <img className="rounded-md size-56" src="/sorry-puppy.webp" alt="En valp som er lei seg" />
        <Heading size="large">Oops!</Heading>
        <BodyShort>Her har det skjedd en feil</BodyShort>
        <Box borderColor="neutral-subtle" borderRadius="8" borderWidth="1">
          <VStack padding="space-8">
            <BodyShort>
              Tittel: <i>{title}</i>
            </BodyShort>
            <BodyShort>
              Feilmelding: <i>{detail}</i>
            </BodyShort>
            {isProblemDetail(error) && (
              <>
                <BodyShort>
                  Status: <i>{error.status}</i>
                </BodyShort>
                <BodyShort>
                  TraceId: <i>{error.traceId as string}</i>
                </BodyShort>
              </>
            )}
          </VStack>
        </Box>
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

function isNavAnsattManglerTilgangError(
  error: Partial<ProblemDetail>,
): error is NavAnsattManglerTilgangError {
  return error.type === "mangler-tilgang";
}

function getErrorTitle(error: ErrorType): string {
  if (!error) return "En uventet feil har oppstått";
  if (typeof error === "string") return error;
  if (error instanceof Error) return error.message;
  if ("title" in error) return error.title;
  return "En uventet feil har oppstått";
}

function getErrorDetail(error: ErrorType): string {
  if (!error) return "Vi beklager, men noe gikk galt. Vennligst prøv igjen senere.";
  if (typeof error === "string") return error;
  if ("detail" in error) return error.detail;
  if ("message" in error) return error.message;
  return "Vi beklager, men noe gikk galt. Vennligst prøv igjen senere.";
}
