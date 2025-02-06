import { Alert, VStack } from "@navikt/ds-react";
import { PageHeader } from "../components/PageHeader";

export default function IngenTilgang() {
  return (
    <>
      <PageHeader title="Mangler tilgang" />
      <VStack align="start" gap="4">
        <Alert variant="warning">
          Du mangler tilgang til utbetalingsløsningen. Tilgang delegeres i Altinn som en
          enkeltrettighet av din arbeidsgiver.
          <br />
          Det er enkeltrettigheten <b>Be om utbetaling - Nav Arbeidsmarkedstiltak</b> du må få via
          Altinn.
          <br />
          Når enkeltrettigheten er delegert i Altinn kan du laste siden på nytt og få tilgang.
        </Alert>
      </VStack>
    </>
  );
}
