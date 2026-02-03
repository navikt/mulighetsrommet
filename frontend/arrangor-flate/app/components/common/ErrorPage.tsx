import { BodyShort, Box, Heading, Link, List, VStack } from "@navikt/ds-react";
import { Link as ReactRouterLink, useLocation } from "react-router";

type Props = {
  statusCode: number;
  title?: string;
  errorText?: string;
};

export function ErrorPage(props: Props) {
  const location = useLocation();
  return (
    <Box background="default" borderRadius="8" padding="space-32">
      <BodyShort textColor="subtle" size="small">
        Statuskode {props.statusCode}
      </BodyShort>
      <Heading level="1" size="large" spacing>
        Beklager, noe gikk galt.
      </Heading>
      <BodyShort spacing>{props.errorText}</BodyShort>
      <BodyShort spacing>Du kan prøve å</BodyShort>
      <VStack gap="space-8">
        <List>
          <List.Item>vente noen minutter og laste siden på nytt</List.Item>
          <List.Item>
            <Link as={ReactRouterLink} to={location.state?.from || "/"}>
              gå tilbake til forrige side
            </Link>
          </List.Item>
        </List>
        <BodyShort>Hvis problemet vedvarer, kan du kontakte oss.</BodyShort>
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
