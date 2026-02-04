import { BodyShort, Box, GlobalAlert } from "@navikt/ds-react";
import { PageHeading } from "~/components/common/PageHeading";

export default function IngenTilgang() {
  return (
    <Box padding="space-24" background="default" borderRadius="8">
      <PageHeading title="Mangler tilgang" />
      <GlobalAlert status="error" centered={false}>
        <GlobalAlert.Header>
          <GlobalAlert.Title>Du mangler tilgang til utbetalingsløsningen.</GlobalAlert.Title>
        </GlobalAlert.Header>
        <GlobalAlert.Content>
          <BodyShort spacing>
            Tilgang delegeres i Altinn som en enkeltrettighet av din arbeidsgiver.
          </BodyShort>
          <BodyShort spacing>
            Det er enkeltrettigheten <b>"Be om utbetaling - Nav Arbeidsmarkedstiltak"</b> du må få
            via Altinn.
          </BodyShort>
          <BodyShort>
            Når enkeltrettigheten er delegert i Altinn kan du laste siden på nytt og få tilgang.
          </BodyShort>
        </GlobalAlert.Content>
      </GlobalAlert>
    </Box>
  );
}
