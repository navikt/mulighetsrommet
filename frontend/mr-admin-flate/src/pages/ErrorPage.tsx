import { BodyShort, Box, Heading, Page, VStack } from "@navikt/ds-react";
import { Link, useLocation, useRouteError } from "react-router";
import { PORTEN_URL } from "../constants";
import { ProblemDetail } from "@mr/api-client-v2";

export function ErrorPage() {
  const routeError = useRouteError() as ProblemDetail | undefined;

  const location = useLocation() as { state: { problemDetail: ProblemDetail } };
  const stateError = location.state?.problemDetail;

  const error = routeError || stateError;

  return (
    <Page>
      <Box id="error-page" className="prose w-1/2 m-auto mt-5 p-4 rounded-md">
        <img className="rounded-md size-56" src="/sorry-puppy.webp" alt="En valp som er lei seg" />
        <Heading size="large">Oops!</Heading>
        <BodyShort>Her har det skjedd en feil!</BodyShort>
        <Box borderColor="border-subtle" borderRadius="large" borderWidth="1">
          <VStack padding="2">
            <BodyShort>
              Tittel: <i>{error?.title}</i>
            </BodyShort>
            <BodyShort>
              Feilmelding: <i>{error?.detail}</i>
            </BodyShort>
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
