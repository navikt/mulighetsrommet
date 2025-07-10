import { Alert, BodyShort, VStack } from "@navikt/ds-react";
import css from "../root.module.css";
import { PageHeading } from "~/components/common/PageHeading";

export default function IngenTilgang() {
  return (
    <VStack align="start" gap="4" className={css.side}>
      <PageHeading title="Mangler tilgang" />
      <Alert variant="warning">
        <BodyShort>
          Du mangler tilgang til utbetalingsløsningen. Tilgang delegeres i Altinn som en
          enkeltrettighet av din arbeidsgiver.
        </BodyShort>
        <BodyShort>
          Det er enkeltrettigheten <b>Be om utbetaling - Nav Arbeidsmarkedstiltak</b> du må få via
          Altinn.
        </BodyShort>
        <BodyShort>
          Når enkeltrettigheten er delegert i Altinn kan du laste siden på nytt og få tilgang.
        </BodyShort>
      </Alert>
    </VStack>
  );
}
