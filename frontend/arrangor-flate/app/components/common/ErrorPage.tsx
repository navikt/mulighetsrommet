import { ProblemDetail } from "@api-client";
import { BodyShort, Box, Heading, Link, List, VStack } from "@navikt/ds-react";
import { Link as ReactRouterLink, useLocation } from "react-router";

interface ProblemDetailPageProps {
  error: ProblemDetail;
}

export function ProblemDetailPage({ error }: ProblemDetailPageProps) {
  const location = useLocation();
  return (
    <Box background="default" borderRadius="8" padding="space-32">
      <Heading level="1" size="large" spacing>
        Beklager, noe gikk galt.
      </Heading>
      <Box padding="space-4" borderRadius="8" marginInline="auto">
        <VStack padding="space-4">
          <BodyShort>
            Tittel: <i>{error.title}</i>
          </BodyShort>
          <BodyShort>
            Feilmelding: <i>{error.detail}</i>
          </BodyShort>
          <BodyShort>
            Status: <i>{error.status}</i>
          </BodyShort>
          {"traceId" in error && (
            <BodyShort>
              TraceId: <i>{error.traceId as string}</i>
            </BodyShort>
          )}
        </VStack>
      </Box>
      <BodyShort spacing>Du kan prøve å</BodyShort>
      <VStack gap="space-16">
        <List>
          <List.Item>vente noen minutter og laste siden på nytt</List.Item>
          <List.Item>
            <Link as={ReactRouterLink} to={location.state?.from || "/"}>
              gå tilbake til forrige side
            </Link>
          </List.Item>
        </List>
        <BodyShort>Hvis problemet vedvarer, kan du ta kontakt med oss.</BodyShort>
        <Link href="tel:55553336">Ring oss på 55 55 33 36</Link>
      </VStack>
    </Box>
  );
}

type Props = {
  statusCode?: number;
  title?: string;
  errorText?: string;
};

export function ErrorPage(props: Props) {
  const location = useLocation();
  return (
    <Box background="default" borderRadius="8" padding="space-32">
      {props.statusCode && (
        <BodyShort textColor="subtle" size="small">
          Statuskode {props.statusCode}
        </BodyShort>
      )}
      <Heading level="1" size="large" spacing>
        Beklager, noe gikk galt.
      </Heading>
      {props.errorText?.split("\n").map((errorLine, index) => (
        <BodyShort key={index} spacing>
          {errorLine}
        </BodyShort>
      ))}
      <BodyShort spacing>Du kan prøve å</BodyShort>
      <VStack gap="space-16">
        <List>
          <List.Item>vente noen minutter og laste siden på nytt</List.Item>
          <List.Item>
            <Link as={ReactRouterLink} to={location.state?.from || "/"}>
              gå tilbake til forrige side
            </Link>
          </List.Item>
        </List>
        <BodyShort>Hvis problemet vedvarer, kan du ta kontakt med oss.</BodyShort>
        <Link href="tel:55553336">Ring oss på 55 55 33 36</Link>
      </VStack>
    </Box>
  );
}

export function ErrorPageNotFound({ errorText }: { errorText?: string }) {
  return (
    <Box background="default" borderRadius="8" padding="space-32">
      <BodyShort textColor="subtle" size="small">
        Statuskode 404
      </BodyShort>
      <Heading level="1" size="large" spacing>
        Beklager, vi fant ikke siden
      </Heading>
      <BodyShort spacing>
        Denne siden kan være slettet eller flyttet, eller det er en feil i lenken.
      </BodyShort>
      {errorText && (
        <BodyShort spacing>
          <b>Feilmelding:</b> {errorText}
        </BodyShort>
      )}
      <Link as={ReactRouterLink} to="/">
        Gå til forsiden
      </Link>
    </Box>
  );
}
