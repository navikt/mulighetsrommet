import { Alert, VStack } from "@navikt/ds-react";
import { PageHeader } from "../components/PageHeader";

export default function IngenTilgang() {
  return (
    <>
      <PageHeader title="Tilgjengelige refusjonskrav" />
      <VStack align="center" gap="4">
        <Alert variant="warning">
          Du mangler tilganger til refusjonsløsningen. Tilganger delegeres i Altinn av din
          arbeidsgiver.
        </Alert>
      </VStack>
    </>
  );
}
