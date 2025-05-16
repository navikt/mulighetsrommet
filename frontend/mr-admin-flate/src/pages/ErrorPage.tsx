import { BodyShort, Box, Heading, Page, VStack } from "@navikt/ds-react";
import { Link, useLocation, useRouteError } from "react-router";
import { PORTEN_URL } from "../constants";
import { ProblemDetail } from "@mr/api-client-v2";
import { IngenLesetilgang } from "@/IngenLesetilgang";

interface GenericError {
  message?: string;
  status?: number;
  title?: string;
  detail?: string;
}

export function ErrorPage() {
  const routeError = useRouteError() as ProblemDetail | GenericError | undefined;
  const location = useLocation() as {
    state: { problemDetail?: ProblemDetail; error?: GenericError };
  };
  const stateError = location.state?.problemDetail || location.state?.error;

  const error = routeError || stateError;

  const getErrorTitle = () => {
    if (!error) return "En uventet feil har oppstått";
    if (error instanceof Error) return error.message;
    if (typeof error === "string") return error;
    if ("title" in error) return error.title;
    return "En uventet feil har oppstått";
  };

  const getErrorDetail = () => {
    if (!error) return "Vi beklager, men noe gikk galt. Vennligst prøv igjen senere.";
    if ("detail" in error) return error.detail;
    if ("message" in error) return error.message;
    return "Vi beklager, men noe gikk galt. Vennligst prøv igjen senere.";
  };

  if (error?.status === 403) return <IngenLesetilgang message={getErrorDetail()} />;

  return (
    <Page>
      <Box id="error-page" className="prose w-1/2 m-auto mt-5 p-4 rounded-md">
        <img className="rounded-md size-56" src="/sorry-puppy.webp" alt="En valp som er lei seg" />
        <Heading size="large">Oops!</Heading>
        <BodyShort>Her har det skjedd en feil</BodyShort>
        <Box borderColor="border-subtle" borderRadius="large" borderWidth="1">
          <VStack padding="2">
            <BodyShort>
              Tittel: <i>{getErrorTitle()}</i>
            </BodyShort>
            <BodyShort>
              Feilmelding: <i>{getErrorDetail()}</i>
            </BodyShort>
            {error && "status" in error && (
              <BodyShort>
                Status: <i>{error.status}</i>
              </BodyShort>
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
