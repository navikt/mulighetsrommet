import { Alert, VStack } from "@navikt/ds-react";
import css from "../root.module.css";
import { PageHeading } from "~/components/common/PageHeading";

export default function IngenTilgang() {
  return (
    <VStack align="start" gap="4" className={css.side}>
      <PageHeading title="Mangler tilgang" />
      <Alert variant="warning">
        <p>
          Du mangler tilgang til utbetalingsløsningen. Tilgang delegeres i Altinn som en
          enkeltrettighet av din arbeidsgiver.
        </p>
        <p>
          Det er enkeltrettigheten <b>Be om utbetaling - Nav Arbeidsmarkedstiltak</b> du må få via
          Altinn.
        </p>
        <p>Når enkeltrettigheten er delegert i Altinn kan du laste siden på nytt og få tilgang.</p>
      </Alert>
    </VStack>
  );
}
