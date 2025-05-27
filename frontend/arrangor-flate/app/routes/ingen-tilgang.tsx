import { Alert, VStack } from "@navikt/ds-react";
import { PageHeader } from "../components/PageHeader";
import css from "../root.module.css";

export default function IngenTilgang() {
  return (
    <VStack align="start" gap="4" className={css.side}>
      <PageHeader title="Mangler tilgang" />
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
  );
}
